package squote.service;

import com.futu.openapi.*;
import com.futu.openapi.pb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import squote.SquoteConstants;
import squote.domain.Execution;
import squote.domain.Market;
import squote.domain.Order;
import squote.domain.StockQuote;
import squote.scheduletask.FutuClientConfig;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static squote.SquoteConstants.Side.BUY;
import static squote.SquoteConstants.Side.SELL;

public class FutuAPIClient implements FTSPI_Trd, FTSPI_Qot, FTSPI_Conn, IBrokerAPIClient {
	protected final Logger log = LoggerFactory.getLogger(getClass());
	private final FTAPI_Conn_Trd futuConnTrd;
	private final FTAPI_Conn_Qot futuConnQot;
	private long errorCode = -1;
	private final Map<Integer, Object> resultMap = new ConcurrentHashMap<>();
	private final FutuClientConfig clientConfig;

	int timeoutSeconds = 30;
	private int onInitConnectCount = 0;
	List<Integer> pendingOrderStatuses = List.of(
			TrdCommon.OrderStatus.OrderStatus_Unknown_VALUE,
			TrdCommon.OrderStatus.OrderStatus_Unsubmitted_VALUE,
			TrdCommon.OrderStatus.OrderStatus_WaitingSubmit_VALUE,
			TrdCommon.OrderStatus.OrderStatus_Submitting_VALUE,
			TrdCommon.OrderStatus.OrderStatus_Submitted_VALUE,
			TrdCommon.OrderStatus.OrderStatus_Filled_Part_VALUE,
			TrdCommon.OrderStatus.OrderStatus_Cancelling_Part_VALUE,
			TrdCommon.OrderStatus.OrderStatus_Cancelling_All_VALUE
	);

	public FutuAPIClient(FutuClientConfig clientConfig, FTAPI_Conn_Trd futuConnTrd, FTAPI_Conn_Qot futuConnQot, String rsaKey, boolean waitConnected) {
		this.clientConfig = clientConfig;

		FTAPI.init();

		byte[] decodedBytes = Base64.getDecoder().decode(rsaKey);
		var decodedRsaKey = new String(decodedBytes);
		decodedRsaKey = decodedRsaKey.replace("\\n", "\n");
		log.info("decodedRsaKey\n{}", decodedRsaKey);
		futuConnTrd.setClientInfo("squote", 1);
		futuConnTrd.setConnSpi(this);
		futuConnTrd.setTrdSpi(this);
		futuConnTrd.setRSAPrivateKey(decodedRsaKey);
		this.futuConnTrd = futuConnTrd;

		futuConnQot.setClientInfo("squote", 1);
		futuConnQot.setConnSpi(this);
		futuConnQot.setQotSpi(this);
		futuConnQot.setRSAPrivateKey(decodedRsaKey);
		this.futuConnQot = futuConnQot;

		var ip = clientConfig.ip();
		var port = clientConfig.port();
		futuConnTrd.initConnect(ip, port, true);
		futuConnQot.initConnect(ip, port, true);

		Instant timeout = Instant.now().plusSeconds(timeoutSeconds);
		while (waitConnected && timeout.isAfter(Instant.now()) && !isConnected()) {
			log.info("waiting for connected");
			try {
				Thread.sleep(500);
			} catch (InterruptedException ignored) {}
		}
		log.info("isConnected={}", isConnected());
	}

	public boolean isConnected() { return errorCode == 0 && onInitConnectCount >= 2; }

	@Override
	public void onInitConnect(FTAPI_Conn client, long errorCode, String desc)
	{
		this.errorCode = errorCode;
		this.onInitConnectCount++;
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
			resultMap.put(seq, null);
		}

		var accountList = response.getS2C().getAccListList();
		log.info("{} account returned", accountList.size());
		resultMap.put(seq, accountList);
	}

	@Override
	public void onReply_GetOrderFillList(FTAPI_Conn client, int seq, TrdGetOrderFillList.Response response) {
		if (response.getRetType() != 0) {
			log.error("TrdGetOrderFillList failed: {}", response.getRetMsg());
			resultMap.put(seq, null);
		}

		var executions = response.getS2C().getOrderFillListList();
		log.info("Seq[{}] {} order returned", seq, executions.size());
		resultMap.put(seq, executions);
	}

	public void close() {
		futuConnTrd.close();
		futuConnQot.close();
	}

	@Override
	public void onReply_GetHistoryOrderFillList(FTAPI_Conn client, int seq, TrdGetHistoryOrderFillList.Response response) {
		if (response.getRetType() != 0) {
			log.error("GetHistoryOrderFillList failed: {}", response.getRetMsg());
			resultMap.put(seq, null);
		}
		var executions = response.getS2C().getOrderFillListList();
		log.info("Seq[{}] {} execution returned", seq, executions.size());
		resultMap.put(seq, executions);
	}

	@Override
	public void onReply_GetOrderList(FTAPI_Conn client, int seq, TrdGetOrderList.Response response) {
		if (response.getRetType() != 0) {
			log.error("TrdGetOrderList failed: {}", response.getRetMsg());
			resultMap.put(seq, null);
		}

		var orders = response.getS2C().getOrderListList();
		log.info("Seq[{}] {} orders returned", seq, orders.size());
		resultMap.put(seq, orders);
	}

	@Override
	public void onReply_PlaceOrder(FTAPI_Conn client, int seq, TrdPlaceOrder.Response response) {
		var result = new PlaceOrderResponse(response.getS2C().getOrderID(), response.getErrCode(), response.getRetMsg());
		if (response.getRetType() != 0) {
			log.error("TrdPlaceOrder failed: {}", response.getRetMsg());
		}
		resultMap.put(seq, result);
	}

	@Override
	public void onReply_ModifyOrder(FTAPI_Conn client, int seq, TrdModifyOrder.Response response) {
		var result = new CancelOrderResponse(response.getErrCode(), response.getRetMsg());
		if (response.getRetType() != 0) {
			log.error("TrdModifyOrder failed: {}", response.getRetMsg());
		}
		resultMap.put(seq, result);
	}

	@Override
	public void onReply_UnlockTrade(FTAPI_Conn client, int seq, TrdUnlockTrade.Response response) {
		if (response.getRetType() != 0) {
			log.error("UnlockTrade failed: {}", response.getRetMsg());
			resultMap.put(seq, false);
		}

		log.info("Seq[{}] unlock trade result={}", seq, response.getRetMsg());
		resultMap.put(seq, true);
	}

	@Override
	public void onReply_GetSecuritySnapshot(FTAPI_Conn client, int seq, QotGetSecuritySnapshot.Response response) {
		if (response.getRetType() != 0) {
			log.error("QotGetSecuritySnapshot failed: {}", response.getRetMsg());
			resultMap.put(seq, null);
		}

		log.info("Seq[{}] QotGetSecuritySnapshot result={}", seq, response.getRetMsg());
		resultMap.put(seq, response.getS2C().getSnapshotList(0));
	}

	public boolean unlockTrade(String code) {
		TrdUnlockTrade.C2S c2s = TrdUnlockTrade.C2S.newBuilder()
				.setPwdMD5(code)
				.setUnlock(true)
				.setSecurityFirm(TrdCommon.SecurityFirm.SecurityFirm_FutuSecurities_VALUE)
				.build();
		TrdUnlockTrade.Request req = TrdUnlockTrade.Request.newBuilder().setC2S(c2s).build();
		int seq = futuConnTrd.unlockTrade(req);
		log.info("Seq[{}] Send unlockTrade", seq);

		return (boolean) getResult(seq);
	}

	public Map<String, Execution> getStockTodayExecutions(Market market) {
		int seq = sendGetTodayOrderFillRequest(toTrdMarket(market));
		log.info("Seq[{}] Send getTodayOrderFillList", seq);

		var result = (List<TrdCommon.OrderFill>) getResult(seq);
		var executions = new HashMap<String, Execution>();
		for (var exec : Objects.requireNonNull(result))
			executions.merge(Long.toString(exec.getOrderID()),
					toExecution(exec),
					Execution::addExecution);
		return executions;
	}

	private static TrdCommon.TrdMarket toTrdMarket(Market market) {
        return switch (market) {
            case HK -> TrdCommon.TrdMarket.TrdMarket_HK;
            case US -> TrdCommon.TrdMarket.TrdMarket_US;
        };
	}

	private static TrdCommon.TrdSecMarket toSecTrdMarket(Market market) {
		return switch (market) {
			case HK -> TrdCommon.TrdSecMarket.TrdSecMarket_HK;
			case US -> TrdCommon.TrdSecMarket.TrdSecMarket_US;
		};
	}

	public HashMap<String, Execution> getStockExecutions(Date fromDate, Market market) {
		int seq = sendGetHistoryOrderFillRequest(fromDate, toTrdMarket(market));
		log.info("Seq[{}] Send getHistoryOrderFillList", seq);

		var result = (List<TrdCommon.OrderFill>) getResult(seq);
		var executions = new HashMap<String, Execution>();
		for (var exec : Objects.requireNonNull(result))
			executions.merge(Long.toString(exec.getOrderID()),
					toExecution(exec),
					Execution::addExecution);
		return executions;
	}

	public PlaceOrderResponse placeOrder(SquoteConstants.Side side, String code, int quantity, double price) {
		int seq = placeOrderRequest(side, code, quantity, price);
		log.info("Seq[{}] Send placeOrder", seq);
		return (PlaceOrderResponse) getResult(seq);
	}

	public CancelOrderResponse cancelOrder(long orderId, String code) {
		var market = Market.getMarketByStockCode(code);
		int seq = cancelOrderRequest(orderId, toTrdMarket(market));
		log.info("Seq[{}] Send cancelOrder", seq);
		return (CancelOrderResponse) getResult(seq);
	}

	@Override
	public List<Order> getPendingOrders(Market market) {
		int seq = sendGetOrderRequest(toTrdMarket(market));
		log.info("Seq[{}] Send getPendingOrders", seq);

		var result = (List<TrdCommon.Order>) getResult(seq);
		if (result == null) return Collections.emptyList();
		return result.stream()
				.peek(o -> log.info("order received [{}]", o.toString().replaceAll("\n", " ")))
				.filter(o -> pendingOrderStatuses.contains(o.getOrderStatus()))
				.map(this::toOrder).toList();
	}

	public StockQuote getStockQuote(String code) {
		int seq = requestQuoteSnapshot(code);
		log.info("Seq[{}] Send requestQuoteSnapshot", seq);

		var result = (QotGetSecuritySnapshot.Snapshot) getResult(seq);
		if (result == null) return null;

		var quote = new StockQuote(code);
		quote.setPrice(String.valueOf(result.getBasic().getCurPrice()));
		quote.setHigh(String.valueOf(result.getBasic().getHighPrice()));
		quote.setLow(String.valueOf(result.getBasic().getLowPrice()));
		quote.setLastUpdate(result.getBasic().getUpdateTime());
		return quote;
	}

	private String toSquoteCode(String futuCode, int trdMarketValue) {
		var code = futuCode.replaceAll("^0+(?!$)", "");
		if (trdMarketValue == TrdCommon.TrdMarket.TrdMarket_US_VALUE) {
			return code + "." + Market.US;
		}
		return code;
	}

	private Order toOrder(TrdCommon.Order order) {
		return new Order(
				toSquoteCode(order.getCode(), order.getTrdMarket()),
				order.getTrdSide() == TrdCommon.TrdSide.TrdSide_Buy_VALUE ? BUY : SELL,
				(int) order.getQty(), order.getPrice(),
				order.getOrderID(),
				(int) order.getFillQty(), order.getFillAvgPrice(),
				new Date((long) order.getCreateTimestamp())
		);
	}

	private Execution toExecution(TrdCommon.OrderFill fill) {
		var exec = new Execution();
		var market = secMarketToMarket(fill.getSecMarket());
		exec.setOrderId(String.valueOf(fill.getOrderID()));
		exec.setFillIds(String.valueOf(fill.getFillID()));
		exec.setQuantity(BigDecimal.valueOf(fill.getQty()));
		exec.setPrice(BigDecimal.valueOf(fill.getPrice()));
		exec.setSide(fill.getTrdSide() == TrdCommon.TrdSide.TrdSide_Buy_VALUE ? BUY : SELL);
		exec.setCode(toSquoteCode(fill.getCode(), fill.getTrdMarket()));
		exec.setTime(getExecutionTime(fill.getCreateTime(), market));
		exec.setMarket(market);
		return exec;
	}

	private long getExecutionTime(String timeStr, Market market) {
		var zoneId = switch (market) {
            case US -> ZoneId.of("America/New_York");
            case HK -> ZoneId.of("Asia/Hong_Kong");
        };
		var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
		var localDateTime = LocalDateTime.parse(timeStr, formatter);
		return localDateTime.atZone(zoneId).toInstant().toEpochMilli();
	}

	private Market secMarketToMarket(int secMarket) {
		return switch (secMarket) {
			case TrdCommon.TrdSecMarket.TrdSecMarket_HK_VALUE -> Market.HK;
			case TrdCommon.TrdSecMarket.TrdSecMarket_US_VALUE -> Market.US;
            default -> throw new IllegalStateException("Unexpected value: " + secMarket);
        };
	}

	private int sendGetTodayOrderFillRequest(TrdCommon.TrdMarket trdMarket) {
		TrdCommon.TrdHeader header = TrdCommon.TrdHeader.newBuilder()
				.setAccID(clientConfig.accountId())
				.setTrdEnv(TrdCommon.TrdEnv.TrdEnv_Real_VALUE)
				.setTrdMarket(trdMarket.getNumber())
				.build();
		TrdGetOrderFillList.C2S c2s = TrdGetOrderFillList.C2S.newBuilder()
				.setHeader(header)
				.build();
		TrdGetOrderFillList.Request req = TrdGetOrderFillList.Request.newBuilder().setC2S(c2s).build();
		return futuConnTrd.getOrderFillList(req);
	}

	private int sendGetHistoryOrderFillRequest(Date fromDate, TrdCommon.TrdMarket trdMarket) {
		var timezone = switch (trdMarket) {
            case TrdMarket_HK -> TimeZone.getTimeZone("Asia/Hong_Kong");
            case TrdMarket_US -> TimeZone.getTimeZone(ZoneId.of("America/New_York"));
			default -> TimeZone.getTimeZone("UTC");
        };
		var dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		dateFormat.setTimeZone(timezone);
		var anAfterNow = Date.from(LocalDateTime.now().plusHours(1).atZone(timezone.toZoneId()).toInstant());
		var beginTime = dateFormat.format(fromDate);
		var endTime = dateFormat.format(anAfterNow);
		log.info("Get history order between {} and {}", beginTime, endTime);

		var header = TrdCommon.TrdHeader.newBuilder()
				.setAccID(clientConfig.accountId())
				.setTrdEnv(TrdCommon.TrdEnv.TrdEnv_Real_VALUE)
				.setTrdMarket(trdMarket.getNumber())
				.build();
		var filter = TrdCommon.TrdFilterConditions.newBuilder()
				.setBeginTime(beginTime)
				.setEndTime(endTime)
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
				(
						a.getAccType() == TrdCommon.TrdAccType.TrdAccType_Margin_VALUE || a.getAccType() == TrdCommon.TrdAccType.TrdAccType_Cash_VALUE)
						&& a.getTrdMarketAuthListList().contains(TrdCommon.TrdMarket.TrdMarket_HK_VALUE)
						&& a.getSimAccType() < 1
				)
				.findFirst().get().getAccID();
	}

	private String toFutuCode(String squoteCode) {
		var market = Market.getMarketByStockCode(squoteCode);
		return switch (market) {
			case HK -> String.format("%05d", Integer.parseInt(squoteCode));
			case US -> Market.getBaseCodeFromTicker(squoteCode);
		};
	}

	private int placeOrderRequest(SquoteConstants.Side side, String code, int quantity, double price) {
		var market = Market.getMarketByStockCode(code);

		TrdCommon.TrdHeader header = TrdCommon.TrdHeader.newBuilder()
				.setAccID(clientConfig.accountId())
				.setTrdEnv(TrdCommon.TrdEnv.TrdEnv_Real_VALUE)
				.setTrdMarket(toTrdMarket(market).getNumber())
				.build();
		TrdPlaceOrder.C2S c2s = TrdPlaceOrder.C2S.newBuilder()
				.setPacketID(futuConnTrd.nextPacketID())
				.setHeader(header)
				.setTrdSide(side == BUY ? TrdCommon.TrdSide.TrdSide_Buy_VALUE : TrdCommon.TrdSide.TrdSide_Sell_VALUE)
				.setOrderType(TrdCommon.OrderType.OrderType_Normal_VALUE)
				.setSecMarket(toSecTrdMarket(market).getNumber())
				.setTimeInForce(TrdCommon.TimeInForce.TimeInForce_GTC_VALUE)
				.setCode(toFutuCode(code))
				.setQty(quantity)
				.setPrice(price)
				.build();

		TrdPlaceOrder.Request req = TrdPlaceOrder.Request.newBuilder().setC2S(c2s).build();
		return futuConnTrd.placeOrder(req);
	}

	private int requestQuoteSnapshot(String code) {
		QotCommon.Security sec = QotCommon.Security.newBuilder()
				.setMarket(QotCommon.QotMarket.QotMarket_HK_Security_VALUE)
				.setCode(String.format("%05d", Integer.parseInt(code)))
				.build();
		QotGetSecuritySnapshot.C2S c2s = QotGetSecuritySnapshot.C2S.newBuilder()
				.addSecurityList(sec)
				.build();
		QotGetSecuritySnapshot.Request req = QotGetSecuritySnapshot.Request.newBuilder().setC2S(c2s).build();
		return futuConnQot.getSecuritySnapshot(req);
	}

	private int cancelOrderRequest(long orderId, TrdCommon.TrdMarket trdMarket) {
		TrdCommon.TrdHeader header = TrdCommon.TrdHeader.newBuilder()
				.setAccID(clientConfig.accountId())
				.setTrdEnv(TrdCommon.TrdEnv.TrdEnv_Real_VALUE)
				.setTrdMarket(trdMarket.getNumber())
				.build();
		TrdModifyOrder.C2S c2s = TrdModifyOrder.C2S.newBuilder()
				.setPacketID(futuConnTrd.nextPacketID())
				.setHeader(header)
				.setOrderID(orderId)
				.setModifyOrderOp(TrdCommon.ModifyOrderOp.ModifyOrderOp_Cancel_VALUE)
				.build();
		TrdModifyOrder.Request req = TrdModifyOrder.Request.newBuilder().setC2S(c2s).build();
		return futuConnTrd.modifyOrder(req);
	}

	private int sendGetOrderRequest(TrdCommon.TrdMarket trdMarket) {
		TrdCommon.TrdHeader header = TrdCommon.TrdHeader.newBuilder()
				.setAccID(clientConfig.accountId())
				.setTrdEnv(TrdCommon.TrdEnv.TrdEnv_Real_VALUE)
				.setTrdMarket(trdMarket.getNumber())
				.build();
		TrdGetOrderList.C2S c2s = TrdGetOrderList.C2S.newBuilder()
				.setHeader(header)
				.build();
		TrdGetOrderList.Request req = TrdGetOrderList.Request.newBuilder().setC2S(c2s).build();
		return futuConnTrd.getOrderList(req);
	}

	private Object getResult(int seq) {
		Instant timeout = Instant.now().plusSeconds(timeoutSeconds);
		while (timeout.isAfter(Instant.now())) {
			var result = resultMap.get(seq);
			if (result != null) {
				return resultMap.remove(seq);
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
    
