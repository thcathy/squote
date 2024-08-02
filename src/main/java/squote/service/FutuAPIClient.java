package squote.service;

import com.futu.openapi.*;
import com.futu.openapi.pb.TrdCommon;
import com.futu.openapi.pb.TrdGetAccList;
import com.futu.openapi.pb.TrdGetHistoryOrderFillList;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import squote.domain.Execution;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static squote.SquoteConstants.Side.BUY;
import static squote.SquoteConstants.Side.SELL;

public class FutuAPIClient implements FTSPI_Trd, FTSPI_Conn {
	protected final Logger log = LoggerFactory.getLogger(getClass());
	private final FTAPI_Conn_Trd futuConnTrd;
	private long errorCode = -1;
	private Map<Integer, WeakReference<Object>> resultMap = new ConcurrentHashMap<>();

	int timeoutSeconds = 30;

	public FutuAPIClient(@NotNull FTAPI_Conn_Trd futuConnTrd, String ip, short port) {
		this.futuConnTrd = futuConnTrd;
		futuConnTrd.setClientInfo("javaclient", 1);
		futuConnTrd.setConnSpi(this);
		futuConnTrd.setTrdSpi(this);
		FTAPI.init();
		futuConnTrd.initConnect(ip, port, false);
	}

	public boolean isConnected() { return errorCode == 0; }

	@Override
	public void onInitConnect(FTAPI_Conn client, long errorCode, String desc)
	{
		this.errorCode = errorCode;
		log.info("onInitConnect: code={} desc={} connID={}", errorCode, desc, client.getConnectID());
	}

	@Override
	public void onDisconnect(FTAPI_Conn client, long errorCode) {
		this.errorCode = errorCode;
		log.info("onDisconnect: code={}", errorCode);
	}

	@Override
	public void onReply_GetAccList(FTAPI_Conn client, int seq, TrdGetAccList.Response response) {
		if (response.getRetType() != 0) {
			log.error("GetAccList failed: {}", response.getRetMsg());
			resultMap.put(seq, new WeakReference<>(null));
		}

		var accountList = response.getS2C().getAccListList();
		log.info("{} account returned", accountList.size());
		resultMap.put(seq, new WeakReference<>(accountList));
	}

	@Override
	public void onReply_GetHistoryOrderFillList(FTAPI_Conn client, int seq, TrdGetHistoryOrderFillList.Response response) {
		if (response.getRetType() != 0) {
			log.error("GetHistoryOrderFillList failed: {}", response.getRetMsg());
			resultMap.put(seq, new WeakReference<>(null));
		}
		var executions = response.getS2C().getOrderFillListList();
		log.info("Seq[{}] {} execution returned", seq, executions.size());
		resultMap.put(seq, new WeakReference<>(executions));
	}

	public HashMap<String, Execution> getHKStockExecutions(long accountId, Date fromDate) {
		int seq = sendGetHistoryOrderFillRequest(accountId, fromDate);
		log.info("Seq[{}] Send getHistoryOrderFillList", seq);

		var result = (List<TrdCommon.OrderFill>) getResult(seq);
		var executions = new HashMap<String, Execution>();
		for (var exec : Objects.requireNonNull(result))
			executions.merge(Long.toString(exec.getOrderID()),
					toExecution(exec),
					Execution::addExecution);
		return executions;
	}

	private Execution toExecution(TrdCommon.OrderFill fill) {
		var exec = new Execution();
		exec.setOrderId(String.valueOf(fill.getOrderID()));
		exec.setFillIds(String.valueOf(fill.getFillID()));
		exec.setQuantity(BigDecimal.valueOf(fill.getQty()));
		exec.setPrice(BigDecimal.valueOf(fill.getPrice()));
		exec.setSide(fill.getTrdSide() == TrdCommon.TrdSide.TrdSide_Buy_VALUE ? BUY : SELL);
		exec.setCode(fill.getCode());
		exec.setTime((long) (fill.getUpdateTimestamp() * 1000));
		return exec;
	}

	private int sendGetHistoryOrderFillRequest(long accountId, Date fromDate) {
		var dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		var header = TrdCommon.TrdHeader.newBuilder()
				.setAccID(accountId)
				.setTrdEnv(TrdCommon.TrdEnv.TrdEnv_Real_VALUE)
				.setTrdMarket(TrdCommon.TrdMarket.TrdMarket_HK_VALUE)
				.build();
		var filter = TrdCommon.TrdFilterConditions.newBuilder()
				.setBeginTime(dateFormat.format(fromDate))
				.setEndTime(dateFormat.format(new Date()))
				.build();
		TrdGetHistoryOrderFillList.C2S c2s = TrdGetHistoryOrderFillList.C2S.newBuilder()
				.setHeader(header)
				.setFilterConditions(filter)
				.build();
		TrdGetHistoryOrderFillList.Request req = TrdGetHistoryOrderFillList.Request.newBuilder().setC2S(c2s).build();
        return futuConnTrd.getHistoryOrderFillList(req);
	}

	public long getHKStockAccountId() {
		// user id is mandatory but it can be any value
		TrdGetAccList.C2S c2s = TrdGetAccList.C2S.newBuilder().setUserID(1).build();
        TrdGetAccList.Request request = TrdGetAccList.Request.newBuilder().setC2S(c2s).build();
        int seq = futuConnTrd.getAccList(request);
		log.info("Seq[{}] Send getAccList", seq);

		var result = (List<TrdCommon.TrdAcc>) getResult(seq);
		return result.stream().filter(a ->
				(a.getAccType() == TrdCommon.TrdAccType.TrdAccType_Margin_VALUE || a.getAccType() == TrdCommon.TrdAccType.TrdAccType_Cash_VALUE)
				&& a.getTrdMarketAuthListList().contains(TrdCommon.TrdMarket.TrdMarket_HK_VALUE)).findFirst().get().getAccID();
	}

	private Object getResult(int seq) {
		Instant timeout = Instant.now().plusSeconds(timeoutSeconds);
		while (timeout.isAfter(Instant.now())) {
			var result = resultMap.get(seq);
			if (result != null) {
				return resultMap.remove(seq).get();
			}

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				log.error("Seq[{}] unexpected exception", seq, e);
			}
		}

		log.error("Seq[{}] Cannot get result", seq);
		return null;
	}
}
    
