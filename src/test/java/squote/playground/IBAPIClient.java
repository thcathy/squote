package squote.playground;

import com.ib.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import squote.SquoteConstants;
import squote.domain.Execution;
import squote.domain.Market;
import squote.domain.Order;
import squote.domain.StockQuote;
import squote.service.IBrokerAPIClient;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
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

	private final List<Bar> barList = new ArrayList<>();

	public IBAPIClient(String host, int port, int clientId) {
		signal = new EJavaSignal();
		client = new EClientSocket(this, signal);

		log.info("Connecting to IB API at {}:{} with clientId {}", host, port, clientId);
		client.eDisconnect();
		client.eConnect(host, port, clientId);
		if (!isConnected()) {
			throw new RuntimeException("Failed to connect to IB API within " + connectionTimeoutSeconds + " seconds");
		}

		reader = new EReader(client, signal);
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

	public void searchSPHBExchange() {
		Contract contract = new Contract();
		contract.symbol("SPHB");
		contract.secType("STK");
		contract.currency("USD");
//		contract.exchange("SMART"); // Use SMART routing initially

		client.reqContractDetails(1, contract);
	}

	public void subscribeMarketData() {
		Contract contract = new Contract();
		contract.symbol("SPHB");                    // Symbol
		contract.secType("STK");                   // Security type: Stock/ETF
		contract.currency("USD");                  // Currency
		contract.exchange("SMART");                // Smart routing
//		contract.primaryExch("NASDAQ");            // Primary exchange

		// Market data options (empty list for basic data)
		List<TagValue> mktDataOptions = new ArrayList<>();

		client.reqMktData(1001, contract, "", false, false, mktDataOptions);

		System.out.println("Subscribed market data");
	}

	public void reqHistoricalData() {
		log.info("Requesting historical data for SPHB...");
		
		Contract SPHB = new Contract();
		SPHB.conid(319357119);
		SPHB.symbol("SPHB");
		SPHB.secType("STK");
		SPHB.exchange("ARCA");
		SPHB.currency("USD");

		barList.clear(); // Clear any previous data

		client.reqHistoricalData(
				2,              // ReqId
				SPHB,            // Contract
				"",             // endDateTime - empty means current time
				"1 Y",          // durationStr
				"5 mins",        // Bar size
				"TRADES",       // WhatToShow
				0,              // UseRTH (0 = include data outside regular trading hours)
				2,              // FormatDate (2 = yyyymmdd hh:mm:ss)
				false,          // KeepUpToDate
				null            // ChartOptions
		);
		
		log.info("Historical data request sent for SPHB (reqId=2)");
	}

	@Override
	public List<Order> getPendingOrders(Market market) {
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
	public Map<String, Execution> getStockTodayExecutions(Market market) {
		// TODO: Implement IB-specific today executions logic
		return Collections.emptyMap();
	}

	@Override
	public PlaceOrderResponse placeOrder(SquoteConstants.Side side, String code, int quantity, double price) {
		// TODO: Implement IB-specific place order logic
		return new PlaceOrderResponse(0, -1, "Not implemented");
	}

	@Override
	public CancelOrderResponse cancelOrder(long orderId, String code) {
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
		log.info("Tick price received: tickerId={}, field={}, price={}", tickerId, field, price);
	}

	@Override
	public void tickSize(int tickerId, int field, Decimal size) {
		log.info("Tick size received: tickerId={}, field={}, size={}", tickerId, field, size);
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
	public void orderStatus(int i, String s, Decimal decimal, Decimal decimal1, double v, int i1, int i2, double v1, int i3, String s1, double v2) {

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
		log.info("nextValidId:" + orderId);
		currentOrderId = orderId;
	}

	@Override
	public void contractDetails(int reqId, ContractDetails contractDetails) {
		Contract contract = contractDetails.contract();

		log.info("Received contract details for reqId {}: symbol={}, exchange={}, conid={}", 
			reqId, contract.symbol(), contract.exchange(), contract.conid());

		System.out.println("=== TRADITIONAL CONTRACT DETAILS ===");
		System.out.println("Request ID: " + reqId);
		System.out.println("Symbol: " + contract.symbol());
		System.out.println("Exchange: " + contract.exchange());
		System.out.println("Primary Exchange: " + contract.primaryExch());
		System.out.println("Currency: " + contract.currency());
		System.out.println("Security Type: " + contract.secType());
		System.out.println("Contract ID: " + contract.conid());
		System.out.println("Local Symbol: " + contract.localSymbol());
		System.out.println("Long Name: " + contractDetails.longName());
		System.out.println("Market Name: " + contractDetails.marketName());
		System.out.println("Min Tick: " + contractDetails.minTick());
		System.out.println("Valid Exchanges: " + contractDetails.validExchanges());
		System.out.println("===================================");
	}

	@Override
	public void bondContractDetails(int reqId, ContractDetails contractDetails) {

	}

	@Override
	public void contractDetailsEnd(int reqId) {
		log.info("Contract details request completed for reqId {}", reqId);
		System.out.println("=== TRADITIONAL CONTRACT DETAILS END ===");
		System.out.println("Request ID: " + reqId);
		System.out.println("======================================");
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
		log.info("Managed accounts received: {}", accountsList);
	}

	public void requestMarketDataType() {
		log.info("Requesting market data type information...");
		// Request market data type - 1=Live, 2=Frozen, 3=Delayed, 4=Delayed-Frozen
		client.reqMarketDataType(1); // Try live first
	}

	@Override
	public void receiveFA(int faDataType, String xml) {

	}

	@Override
	public void historicalData(int reqId, Bar bar) {
		log.info("Received historical data: reqId={}, time={}, open={}, high={}, low={}, close={}, volume={}", 
			reqId, bar.time(), bar.open(), bar.high(), bar.low(), bar.close(), bar.volume());
		barList.add(bar);
	}

	@Override
	public void scannerParameters(String xml) {

	}

	// Save to CSV file
	private void saveToCSV() throws IOException {
		var fileName = "/tmp/SPHB-US-kline-5m.csv";
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
			writer.write("Time,Open,High,Low,Close,Volume,WAP,Count\n");
			for (Bar bar : barList) {
				writer.write(String.format("%s,%.2f,%.2f,%.2f,%.2f,%.0f,%.4f,%d\n",
						bar.time(), bar.open(), bar.high(), bar.low(), bar.close(), bar.volume(), bar.wap(), bar.count()));
			}
		}
		System.out.println("Saved to " + fileName);
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
		String dataTypeStr;
		switch (marketDataType) {
			case 1: dataTypeStr = "Live"; break;
			case 2: dataTypeStr = "Frozen"; break;
			case 3: dataTypeStr = "Delayed"; break;
			case 4: dataTypeStr = "Delayed-Frozen"; break;
			default: dataTypeStr = "Unknown(" + marketDataType + ")"; break;
		}
		log.info("Market data type for reqId {}: {} ({})", reqId, dataTypeStr, marketDataType);
	}

	@Override
	public void commissionReport(CommissionReport commissionReport) {

	}

	@Override
	public void position(String account, Contract contract, Decimal pos, double avgCost) {

	}

	@Override
	public void positionEnd() {

	}

	@Override
	public void accountSummary(int reqId, String account, String tag, String value, String currency) {
		log.info("Account summary: reqId={}, account={}, tag={}, value={}, currency={}", 
			reqId, account, tag, value, currency);
	}

	@Override
	public void accountSummaryEnd(int reqId) {
		log.info("Account summary request completed for reqId {}", reqId);
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
	public void error(int id, int errorCode, String errorMsg, String advancedOrderRejectJson) {
		log.error("IB API error - id: {}, code: {}, msg: {}, advancedOrderRejectJson: {}", id, errorCode, errorMsg, advancedOrderRejectJson);
		lastErrorCode = errorCode;

		// Handle specific error codes
		if (lastErrorCode == 506) {
			log.error("Server requires newer API version than currently available. Download latest API from: https://interactivebrokers.github.io/");
		} else if (lastErrorCode == 162) {
			log.error("Historical Market Data Service error. This might be due to pacing violations or data subscription issues.");
		} else if (lastErrorCode == 200) {
			log.error("No security definition has been found for the request");
		} else if (lastErrorCode == 321) {
			log.error("Error validating request. Check contract details and parameters.");
		} else if (lastErrorCode == 322) {
			log.error("Error processing request. This might be a server-side issue.");
		} else if (lastErrorCode == 10147) {
			log.error("OrderId {} that needs to be cancelled is not found.", id);
		} else if (lastErrorCode == 10148) {
			log.error("OrderId {} that needs to be cancelled cannot be cancelled, state: {}", id, errorMsg);
		} else if (lastErrorCode == 502) {
			log.error("Couldn't connect to TWS. Confirm that 'Enable ActiveX and Socket Clients' is enabled and connection port is set correctly.");
		}
		
		// If this is a historical data request, complete the operation with error
		if ((id == 2 || id == 3) && (errorCode == 162 || errorCode == 200 || errorCode == 321 || errorCode == 322)) {
			log.error("Historical data request (reqId={}) failed with error code {}, completing operation", id, errorCode);
			completeOperation(Operation.HISTORICAL_DATA);
		}
	}

	@Override
	public void connectionClosed() {
		log.info("IB connection closed");
	}

	@Override
	public void connectAck() {
		log.info("IB connection acknowledged");
		log.info("Server Version: {}", client.serverVersion());
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
		log.info("Historical data request completed: reqId={}, startDate={}, endDate={}, total bars received={}", 
			reqId, startDateStr, endDateStr, barList.size());
		
		// Complete the operation
		completeOperation(Operation.HISTORICAL_DATA);
		
		System.out.println("End of historical data.");
		try {
			saveToCSV();
		} catch (IOException e) {
			log.error("Error saving historical data to CSV", e);
			e.printStackTrace();
		}
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
    
