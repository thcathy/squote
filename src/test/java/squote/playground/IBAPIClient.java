package squote.playground;

import com.ib.client.*;
import com.ib.client.protobuf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import squote.SquoteConstants;
import squote.domain.Execution;
import squote.domain.Order;
import squote.domain.StockQuote;
import squote.service.IBrokerAPIClient;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class IBAPIClient implements IBrokerAPIClient, EWrapper {
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	// Operations that can be tracked for completion
	private enum Operation {PENDING_ORDERS, EXECUTIONS, HISTORICAL_DATA, CONTRACT_DETAILS, STOCK_QUOTE}
	
	private final Map<Integer, Object> resultMap = new ConcurrentHashMap<>();
	private final EJavaSignal signal;
	private final EClientSocket client;
	private final EReader reader;
	protected int currentOrderId = -1;
	int timeoutSeconds = 30;
	int connectionTimeoutSeconds = 5;
	
	// Generic operation tracking
	private Map<Operation, Boolean> operationComplete = new ConcurrentHashMap<>();
	private Map<String, Object> operationData = new ConcurrentHashMap<>();
	private volatile long lastErrorCode = 0;

	public IBAPIClient(String host, int port, int clientId) {
		signal = new EJavaSignal();
		client = new EClientSocket(this, signal);
		reader = new EReader(client, signal);

		log.info("Connecting to IB API at {}:{} with clientId {}", host, port, clientId);
		client.eDisconnect();
		client.eConnect(host, port, clientId);
		
		reader.start();
		new Thread(() -> {
			while (client.isConnected()) {
				signal.waitForSignal();
				try {
					reader.processMsgs();
				} catch (Exception e) {
					log.error("Error processing IB messages", e);
				}
			}
		}).start();

		Instant timeout = Instant.now().plusSeconds(connectionTimeoutSeconds);
		while (timeout.isAfter(Instant.now()) && !isConnected()) {
			log.info("waiting for connected");
			try {
				Thread.sleep(500);
			} catch (InterruptedException ignored) {}
		}
		
		if (!isConnected()) {
			throw new RuntimeException("Failed to connect to IB API within " + connectionTimeoutSeconds + " seconds");
		}
		
		log.info("isConnected={}", isConnected());
	}

	public void close() {
		if (client != null && client.isConnected()) {
			client.eDisconnect();
		}
	}

	public boolean isConnected() {
		return client != null && client.isConnected();
	}

	private boolean waitForCondition(java.util.function.BooleanSupplier condition, String operationName) {
		Instant timeout = Instant.now().plusSeconds(timeoutSeconds);
		while (timeout.isAfter(Instant.now()) && !condition.getAsBoolean()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				log.error("Interrupted while waiting for {}", operationName, e);
				return false;
			}
		}
		
		if (!condition.getAsBoolean()) {
			log.warn("Timeout waiting for {} response", operationName);
			return false;
		}
		
		return true;
	}

	private void startOperation(Operation operation) {
		operationComplete.put(operation, false);
	}

	private void completeOperation(Operation operation) {
		operationComplete.put(operation, true);
	}

	private boolean waitForOperation(Operation operation) {
		return waitForCondition(() -> Boolean.TRUE.equals(operationComplete.get(operation)), operation.name());
	}

	@SuppressWarnings("unchecked")
	private <T> T getOperationData(Operation operation, Class<T> type) {
		String key = operation.name() + "_" + type.getSimpleName().toUpperCase();
		return (T) operationData.computeIfAbsent(key, k -> {
			if (type == List.class) {
				return new CopyOnWriteArrayList<>();
			} else if (type == Map.class) {
				return new ConcurrentHashMap<>();
			} else if (type == Set.class) {
				return ConcurrentHashMap.newKeySet();
			}
			throw new IllegalArgumentException("Unsupported operation data type: " + type);
		});
	}

	@Override
	public List<Order> getPendingOrders() {
		// Request all open orders
		startOperation(Operation.PENDING_ORDERS);
		
		client.reqAllOpenOrders();
		
		// Wait for orders to be populated via openOrder callbacks
		waitForOperation(Operation.PENDING_ORDERS);
		
		List<com.ib.client.Order> pendingOrders = getOperationData(Operation.PENDING_ORDERS, List.class);
		log.info("Retrieved {} pending orders", pendingOrders.size());
		
		return pendingOrders.stream()
				.peek(o -> log.info("order received [orderId={}, symbol={}, action={}, totalQty={}]", 
					o.orderId(), getSymbolFromOrder(o), o.action(), o.totalQuantity()))
				.map(this::toOrder)
				.filter(Objects::nonNull)
				.toList();
	}

	@Override
	public StockQuote getStockQuote(String code) {
		// TODO: Implement IB-specific stock quote logic
		return null;
	}

	@Override
	public Map<String, Execution> getHKStockTodayExecutions() {
		// TODO: Implement IB-specific today executions logic
		return Collections.emptyMap();
	}

	@Override
	public PlaceOrderResponse placeOrder(SquoteConstants.Side side, String code, int quantity, double price) {
		// TODO: Implement IB-specific place order logic
		return new PlaceOrderResponse(0, -1, "Not implemented");
	}

	@Override
	public CancelOrderResponse cancelOrder(long orderId) {
		// TODO: Implement IB-specific cancel order logic
		return new CancelOrderResponse(-1, "Not implemented");
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

	@Override
	public void tickPrice(int tickerId, int field, double price, TickAttrib attrib) {

	}

	@Override
	public void tickSize(int tickerId, int field, Decimal size) {

	}

	@Override
	public void tickOptionComputation(int tickerId, int field, int tickAttrib, double impliedVol, double delta, double optPrice, double pvDividend, double gamma, double vega, double theta, double undPrice) {

	}

	@Override
	public void tickGeneric(int tickerId, int tickType, double value) {

	}

	@Override
	public void tickString(int tickerId, int tickType, String value) {

	}

	@Override
	public void tickEFP(int tickerId, int tickType, double basisPoints, String formattedBasisPoints, double impliedFuture, int holdDays, String futureLastTradeDate, double dividendImpact, double dividendsToLastTradeDate) {

	}

	@Override
	public void orderStatus(int i, String s, Decimal decimal, Decimal decimal1, double v, long l, int i1, double v1, int i2, String s1, double v2) {

	}

	@Override
	public void openOrder(int orderId, Contract contract, com.ib.client.Order order, OrderState orderState) {
		log.debug("Received open order: orderId={}, symbol={}, action={}, qty={}", 
			orderId, contract.symbol(), order.action(), order.totalQuantity());
		
		List<com.ib.client.Order> pendingOrders = getOperationData(Operation.PENDING_ORDERS, List.class);
		pendingOrders.add(order);
		
		Map<Integer, Contract> orderContracts = getOperationData(Operation.PENDING_ORDERS, Map.class);
		orderContracts.put(orderId, contract);
	}

	@Override
	public void openOrderEnd() {
		log.debug("Open orders request completed");
		completeOperation(Operation.PENDING_ORDERS);
	}

	@Override
	public void updateAccountValue(String key, String value, String currency, String accountName) {

	}

	@Override
	public void updatePortfolio(Contract contract, Decimal position, double marketPrice, double marketValue, double averageCost, double unrealizedPNL, double realizedPNL, String accountName) {

	}

	@Override
	public void updateAccountTime(String timeStamp) {

	}

	@Override
	public void accountDownloadEnd(String accountName) {

	}

	@Override
	public void nextValidId(int orderId) {
		currentOrderId = orderId;
	}

	@Override
	public void contractDetails(int reqId, ContractDetails contractDetails) {

	}

	@Override
	public void bondContractDetails(int reqId, ContractDetails contractDetails) {

	}

	@Override
	public void contractDetailsEnd(int reqId) {

	}

	@Override
	public void execDetails(int reqId, Contract contract, com.ib.client.Execution execution) {

	}

	@Override
	public void execDetailsEnd(int reqId) {

	}

	@Override
	public void updateMktDepth(int tickerId, int position, int operation, int side, double price, Decimal size) {

	}

	@Override
	public void updateMktDepthL2(int tickerId, int position, String marketMaker, int operation, int side, double price, Decimal size, boolean isSmartDepth) {

	}

	@Override
	public void updateNewsBulletin(int msgId, int msgType, String message, String origExchange) {

	}

	@Override
	public void managedAccounts(String accountsList) {

	}

	@Override
	public void receiveFA(int faDataType, String xml) {

	}

	@Override
	public void historicalData(int reqId, Bar bar) {

	}

	@Override
	public void scannerParameters(String xml) {

	}

	@Override
	public void scannerData(int reqId, int rank, ContractDetails contractDetails, String distance, String benchmark, String projection, String legsStr) {

	}

	@Override
	public void scannerDataEnd(int reqId) {

	}

	@Override
	public void realtimeBar(int reqId, long time, double open, double high, double low, double close, Decimal volume, Decimal wap, int count) {

	}

	@Override
	public void currentTime(long time) {

	}

	@Override
	public void fundamentalData(int reqId, String data) {

	}

	@Override
	public void deltaNeutralValidation(int reqId, DeltaNeutralContract deltaNeutralContract) {

	}

	@Override
	public void tickSnapshotEnd(int reqId) {

	}

	@Override
	public void marketDataType(int reqId, int marketDataType) {

	}

	@Override
	public void commissionAndFeesReport(CommissionAndFeesReport commissionAndFeesReport) {

	}

	@Override
	public void position(String account, Contract contract, Decimal pos, double avgCost) {

	}

	@Override
	public void positionEnd() {

	}

	@Override
	public void accountSummary(int reqId, String account, String tag, String value, String currency) {

	}

	@Override
	public void accountSummaryEnd(int reqId) {

	}

	@Override
	public void verifyMessageAPI(String apiData) {

	}

	@Override
	public void verifyCompleted(boolean isSuccessful, String errorText) {

	}

	@Override
	public void verifyAndAuthMessageAPI(String apiData, String xyzChallenge) {

	}

	@Override
	public void verifyAndAuthCompleted(boolean isSuccessful, String errorText) {

	}

	@Override
	public void displayGroupList(int reqId, String groups) {

	}

	@Override
	public void displayGroupUpdated(int reqId, String contractInfo) {

	}

	@Override
	public void error(Exception e) {
		log.error("IB API error", e);
	}

	@Override
	public void error(String str) {
		log.error("IB API error: {}", str);
	}

	@Override
	public void error(int id, long errorTime, int errorCode, String errorMsg, String advancedOrderRejectJson) {
		log.error("IB API error - id: {}, code: {}, msg: {}, advancedOrderRejectJson: {}", id, errorCode, errorMsg, advancedOrderRejectJson);
		lastErrorCode = errorCode;

		if (lastErrorCode == 506) {
			log.error("Server requires newer API version than currently available. Download latest API from: https://interactivebrokers.github.io/");
		}
	}

	@Override
	public void connectionClosed() {
		log.info("IB connection closed");
	}

	@Override
	public void connectAck() {
		log.info("IB connection acknowledged");
	}

	@Override
	public void positionMulti(int reqId, String account, String modelCode, Contract contract, Decimal pos, double avgCost) {

	}

	@Override
	public void positionMultiEnd(int reqId) {

	}

	@Override
	public void accountUpdateMulti(int reqId, String account, String modelCode, String key, String value, String currency) {

	}

	@Override
	public void accountUpdateMultiEnd(int reqId) {

	}

	@Override
	public void securityDefinitionOptionalParameter(int reqId, String exchange, int underlyingConId, String tradingClass, String multiplier, Set<String> expirations, Set<Double> strikes) {

	}

	@Override
	public void securityDefinitionOptionalParameterEnd(int reqId) {

	}

	@Override
	public void softDollarTiers(int reqId, SoftDollarTier[] tiers) {

	}

	@Override
	public void familyCodes(FamilyCode[] familyCodes) {

	}

	@Override
	public void symbolSamples(int reqId, ContractDescription[] contractDescriptions) {

	}

	@Override
	public void historicalDataEnd(int reqId, String startDateStr, String endDateStr) {

	}

	@Override
	public void mktDepthExchanges(DepthMktDataDescription[] depthMktDataDescriptions) {

	}

	@Override
	public void tickNews(int tickerId, long timeStamp, String providerCode, String articleId, String headline, String extraData) {

	}

	@Override
	public void smartComponents(int reqId, Map<Integer, Map.Entry<String, Character>> theMap) {

	}

	@Override
	public void tickReqParams(int tickerId, double minTick, String bboExchange, int snapshotPermissions) {

	}

	@Override
	public void newsProviders(NewsProvider[] newsProviders) {

	}

	@Override
	public void newsArticle(int requestId, int articleType, String articleText) {

	}

	@Override
	public void historicalNews(int requestId, String time, String providerCode, String articleId, String headline) {

	}

	@Override
	public void historicalNewsEnd(int requestId, boolean hasMore) {

	}

	@Override
	public void headTimestamp(int reqId, String headTimestamp) {

	}

	@Override
	public void histogramData(int reqId, List<HistogramEntry> items) {

	}

	@Override
	public void historicalDataUpdate(int reqId, Bar bar) {

	}

	@Override
	public void rerouteMktDataReq(int reqId, int conId, String exchange) {

	}

	@Override
	public void rerouteMktDepthReq(int reqId, int conId, String exchange) {

	}

	@Override
	public void marketRule(int marketRuleId, PriceIncrement[] priceIncrements) {

	}

	@Override
	public void pnl(int reqId, double dailyPnL, double unrealizedPnL, double realizedPnL) {

	}

	@Override
	public void pnlSingle(int reqId, Decimal pos, double dailyPnL, double unrealizedPnL, double realizedPnL, double value) {

	}

	@Override
	public void historicalTicks(int reqId, List<HistoricalTick> ticks, boolean done) {

	}

	@Override
	public void historicalTicksBidAsk(int reqId, List<HistoricalTickBidAsk> ticks, boolean done) {

	}

	@Override
	public void historicalTicksLast(int reqId, List<HistoricalTickLast> ticks, boolean done) {

	}

	@Override
	public void tickByTickAllLast(int reqId, int tickType, long time, double price, Decimal size, TickAttribLast tickAttribLast, String exchange, String specialConditions) {

	}

	@Override
	public void tickByTickBidAsk(int reqId, long time, double bidPrice, double askPrice, Decimal bidSize, Decimal askSize, TickAttribBidAsk tickAttribBidAsk) {

	}

	@Override
	public void tickByTickMidPoint(int reqId, long time, double midPoint) {

	}

	@Override
	public void orderBound(long orderId, int apiClientId, int apiOrderId) {

	}

	@Override
	public void completedOrder(Contract contract, com.ib.client.Order order, OrderState orderState) {

	}

	@Override
	public void completedOrdersEnd() {

	}

	@Override
	public void replaceFAEnd(int reqId, String text) {

	}

	@Override
	public void wshMetaData(int reqId, String dataJson) {

	}

	@Override
	public void wshEventData(int reqId, String dataJson) {

	}

	@Override
	public void historicalSchedule(int reqId, String startDateTime, String endDateTime, String timeZone, List<HistoricalSession> sessions) {

	}

	@Override
	public void userInfo(int reqId, String whiteBrandingId) {

	}

	@Override
	public void currentTimeInMillis(long l) {

	}

	@Override
	public void orderStatusProtoBuf(OrderStatusProto.OrderStatus orderStatus) {

	}

	@Override
	public void openOrderProtoBuf(OpenOrderProto.OpenOrder openOrder) {

	}

	@Override
	public void openOrdersEndProtoBuf(OpenOrdersEndProto.OpenOrdersEnd openOrdersEnd) {

	}

	@Override
	public void errorProtoBuf(ErrorMessageProto.ErrorMessage errorMessage) {

	}

	@Override
	public void execDetailsProtoBuf(ExecutionDetailsProto.ExecutionDetails executionDetails) {

	}

	@Override
	public void execDetailsEndProtoBuf(ExecutionDetailsEndProto.ExecutionDetailsEnd executionDetailsEnd) {

	}

	private String getSymbolFromOrder(com.ib.client.Order order) {
		Map<Integer, Contract> orderContracts = getOperationData(Operation.PENDING_ORDERS, Map.class);
		Contract contract = orderContracts.get(order.orderId());
		return contract != null ? contract.symbol() : "UNKNOWN";
	}

	private Order toOrder(com.ib.client.Order ibOrder) {
		try {
			Map<Integer, Contract> orderContracts = getOperationData(Operation.PENDING_ORDERS, Map.class);
			Contract contract = orderContracts.get(ibOrder.orderId());
			if (contract == null) {
				log.warn("No contract found for order {}", ibOrder.orderId());
				return null;
			}
			
			String code = contract.symbol();
			SquoteConstants.Side side = "BUY".equals(ibOrder.action()) ? 
				SquoteConstants.Side.BUY : SquoteConstants.Side.SELL;
			int quantity = ibOrder.totalQuantity().value().intValue();
			double price = ibOrder.lmtPrice();
			long orderId = ibOrder.orderId();
			int fillQty = ibOrder.filledQuantity().value().intValue();
			double fillAvgPrice = 0.0; // IB doesn't provide this in Order object
			Date createTime = new Date(); // IB doesn't provide creation time in Order object
			
			return new Order(code, side, quantity, price, orderId, fillQty, fillAvgPrice, createTime);
		} catch (Exception e) {
			log.error("Error converting IB order to Order object", e);
			return null;
		}
	}
}
    
