package squote.service.ibkr;

import com.ib.client.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import squote.SquoteConstants;
import squote.domain.Execution;
import squote.domain.Market;
import squote.domain.Order;
import squote.domain.StockQuote;
import squote.service.IBrokerAPIClient;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static squote.SquoteConstants.Side.BUY;
import static squote.SquoteConstants.Side.SELL;

public class IBAPIClient implements IBrokerAPIClient, EWrapper {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private enum Operation {GET_PENDING_ORDERS, GET_EXECUTIONS, HISTORICAL_DATA, CONTRACT_DETAILS, STOCK_QUOTE, PLACE_ORDER}
	private final EJavaSignal signal;
	private final EClientSocket client;
	private final EReader reader;
	protected int currentOrderId = -1;
	int timeoutSeconds = 30;
	int connectionTimeoutSeconds = 5;
	private final Map<Operation, Boolean> operationComplete = new ConcurrentHashMap<>();
	private volatile long lastErrorCode = 0;
	private final List<Bar> barList = new ArrayList<>();
	private final Map<Integer, OrderDetail> orderDetails = new ConcurrentHashMap<>();
	private final Map<Integer, OrderStatusDetail> orderStatusDetails = new ConcurrentHashMap<>();
	private final Map<String, ExecDetail> executions = new ConcurrentHashMap<>();
	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final String reportQueryToken;
	private final String executionReportQueryId;
	private final String flexBaseUrl = "https://ndcdyn.interactivebrokers.com/AccountManagement/FlexWebService";

	// for unit test
	public IBAPIClient(EJavaSignal signal, EClientSocket client, EReader reader, String reportQueryToken, String executionReportQueryId) {
		this.signal = signal;
		this.client = client;
		this.reader = reader;
		this.reportQueryToken = reportQueryToken;
		this.executionReportQueryId = executionReportQueryId;
	}

	public IBAPIClient(String reportQueryToken, String executionReportQueryId) {
		this.signal = null;
		this.client = null;
		this.reader = null;
		this.reportQueryToken = reportQueryToken;
		this.executionReportQueryId = executionReportQueryId;
	}

	public IBAPIClient(String host, int port, int clientId, String reportQueryToken, String executionReportQueryId) {
		this.reportQueryToken = reportQueryToken;
		this.executionReportQueryId = executionReportQueryId;
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

		waitForCondition(() -> currentOrderId > -1);
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

	private boolean waitForCondition(java.util.function.BooleanSupplier condition) {
		Instant timeout = Instant.now().plusSeconds(timeoutSeconds);
		while (timeout.isAfter(Instant.now())) {
			try {
				if (condition.getAsBoolean()) return true;

				Thread.sleep(100);
			} catch (InterruptedException e) {
				log.error("Interrupted while waiting", e);
				return false;
			}
		}

		log.warn("Timeout waiting for response");
		return false;
	}

	private void completeOperation(Operation operation) {
		operationComplete.put(operation, true);
	}

	// comment out until pay for mkt data
//	public void subscribeMarketData() {
		// comment out until pay for mkt data
//		Contract contract = new Contract();
//		contract.symbol("SPHB");                    // Symbol
//		contract.secType("STK");                   // Security type: Stock/ETF
//		contract.currency("USD");                  // Currency
//		contract.exchange("SMART");                // Smart routing
//		List<TagValue> mktDataOptions = new ArrayList<>();
//
//		client.reqMktData(1001, contract, "", false, false, mktDataOptions);
//
//		System.out.println("Subscribed market data");
//	}

//	public void reqHistoricalData() {
//		log.info("Requesting historical data for SPHB...");
//
//		Contract SPHB = new Contract();
//		SPHB.conid(319357119);
//		SPHB.symbol("SPHB");
//		SPHB.secType("STK");
//		SPHB.exchange("ARCA");
//		SPHB.currency("USD");
//
//		barList.clear(); // Clear any previous data
//
//		client.reqHistoricalData(
//				2,              // ReqId
//				SPHB,            // Contract
//				"",             // endDateTime - empty means current time
//				"1 Y",          // durationStr
//				"5 mins",        // Bar size
//				"TRADES",       // WhatToShow
//				0,              // UseRTH (0 = include data outside regular trading hours)
//				2,              // FormatDate (2 = yyyymmdd hh:mm:ss)
//				false,          // KeepUpToDate
//				null            // ChartOptions
//		);
//
//		log.info("Historical data request sent for SPHB (reqId=2)");
//	}

	@Override
	public List<Order> getPendingOrders(Market market) {
		client.reqOpenOrders();

		waitForCondition(() -> operationComplete.getOrDefault(Operation.GET_PENDING_ORDERS, false));

		// workaround for market filtering
		var currency = switch (market) {
			case HK -> "HKD";
			case US -> "USD";
		};

		return orderDetails.entrySet().stream()
				.filter(e -> e.getValue().orderState.status().isActive() && currency.equals(e.getValue().contract.currency()))
				.peek(e -> log.info("order {} received={}", e.getKey(), e.getValue()))
				.map(e -> toOrder(e.getValue(), market)).toList();
	}

	@Override
	public StockQuote getStockQuote(String code) {
		// TODO: Implement IB-specific stock quote logic
		return null;
	}

	@Override
	public Map<String, Execution> getStockTodayExecutions(Market market) {
		executions.clear();

		client.reqExecutions(currentOrderId++, new ExecutionFilter());

		waitForCondition(() ->
				operationComplete.getOrDefault(Operation.GET_EXECUTIONS, false));

		var results = new HashMap<String, Execution>();
		for (var execDetail : executions.values())
			results.merge(Long.toString(execDetail.execution.orderId()),
					toExecution(execDetail),
					Execution::addExecution);

		return Collections.emptyMap();
	}

	private Execution toExecution(ExecDetail execDetail) {
		var exec = new Execution();
		exec.setOrderId(String.valueOf(execDetail.execution.orderId()));
		exec.setFillIds(execDetail.execution.execId());
		exec.setQuantity(execDetail.execution.shares().value());
		exec.setPrice(BigDecimal.valueOf(execDetail.execution.price()));
		exec.setSide(execDetail.execution.side().equals("BUY") ? BUY : SELL);
		exec.setCode(execDetail.contract.symbol() + ".US"); // hardcoded until trading outside US
		exec.setTime(getExecutionTime(execDetail.execution.time(), Market.US)); // hardcoded until trading outside US
		exec.setMarket(Market.US); // hardcoded until trading outside US
		return exec;
	}

	private long getExecutionTime(String timeStr, Market market) {
		var timezone = switch (market) {
			case US -> TimeZone.getTimeZone("America/New_York");
			case HK -> TimeZone.getTimeZone("Asia/Hong_Kong");
		};
		var dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		dateFormat.setTimeZone(timezone);
		try {
			return dateFormat.parse(timeStr).getTime();
		} catch (Exception e) {
			log.error("Failed to parse execution time: {}", timeStr, e);
			return System.currentTimeMillis();
		}
	}

	@Override
	public PlaceOrderResponse placeOrder(SquoteConstants.Side side, String code, int quantity, double price) {
		if (currentOrderId == -1) {
			log.error("No valid order ID available - connection may not be established");
			return new PlaceOrderResponse(0, -1, "No valid order ID - connection not established");
		}

		try {
			var contract = createContract(code);
			var order = createIBOrder(side, quantity, price);
			var orderId = currentOrderId++;
			log.info("Placing order: orderId={}, symbol={}, action={}, qty={}, price={}", 
				orderId, code, side, quantity, price);
			
			client.placeOrder(orderId, contract, order);
			boolean success = waitForCondition(
				() -> orderDetails.containsKey(orderId) && orderDetails.get(orderId).orderState.status().isActive()
			);
			
			if (success) {
				log.info("Order placed successfully: orderId={}", orderId);
				return new PlaceOrderResponse(orderId, 0L, "");
			} else {
				log.error("Order placement failed or timed out: orderId={}", orderId);
				return new PlaceOrderResponse(0, lastErrorCode, "Order placement failed or timed out");
			}
		} catch (Exception e) {
			log.error("Error placing order for {}", code, e);
			return new PlaceOrderResponse(0, -1, "Error placing order: " + e.getMessage());
		}
	}

	@Override
	public CancelOrderResponse cancelOrder(long orderId, String code) {
		var ibOrderId = (int) orderId;
		client.cancelOrder(ibOrderId, new OrderCancel());
		var success = waitForCondition(() ->
				orderStatusDetails.containsKey(ibOrderId)
						&& "Cancelled".equals(orderStatusDetails.get(ibOrderId).status)
		);

		return success
				? new CancelOrderResponse(-1, "Cancelled")
				: new CancelOrderResponse(1, "Cancel failed");
	}

	/**
	 * IB gateway only returns T-day executions
	 * @see <a href="https://www.interactivebrokers.com/campus/ibkr-api-page/twsapi-doc/#exec-details">IB API Documentation</a>
	 */
	@Override
	public Map<String, Execution> getRecentExecutions(Date fromDate, Market market) {
		var dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		var filter = new ExecutionFilter();
		filter.time(dateFormat.format(fromDate));

		executions.clear();
		client.reqExecutions(currentOrderId++, filter);
		waitForCondition(() ->
				operationComplete.getOrDefault(Operation.GET_EXECUTIONS, false));

		var results = new HashMap<String, Execution>();
		for (var execDetail : executions.values())
			results.merge(Long.toString(execDetail.execution.orderId()),
					toExecution(execDetail),
					Execution::addExecution);

		return Collections.emptyMap();
	}

	/**
	 * Get historical executions using IB Flex Web Service
	 * @param fromDate the starting date to retrieve executions from
	 * @param market the market to query executions for
	 * @return map of executions keyed by order ID
	 */
	@Override
	public Map<String, Execution> getHistoricalExecutions(Date fromDate, Market market) {
		try {
			log.info("Requesting historical executions from {} for market {}", fromDate, market);
			var referenceCode = requestFlexReport(fromDate);
			return downloadAndParseFlexReport(referenceCode, market);
		} catch (Exception e) {
			log.error("Error retrieving historical executions", e);
			return Collections.emptyMap();
		}
	}

    @Override
    public Map<Currency, Double> getAvailableFunds() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private String requestFlexReport(Date fromDate) throws IOException, InterruptedException {
		var dateFormat = new SimpleDateFormat("yyyyMMdd");
		var fromDateStr = dateFormat.format(fromDate);
		var calendar = Calendar.getInstance();
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		var tomorrowStr = dateFormat.format(calendar.getTime());
		var requestUrl = String.format("%s/SendRequest?t=%s&q=%s&fd=%s&td=%s&v=3",
				flexBaseUrl, reportQueryToken, executionReportQueryId, fromDateStr, tomorrowStr);
		
		var request = HttpRequest.newBuilder()
			.uri(URI.create(requestUrl))
			.GET().build();
		var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		
		if (response.statusCode() != 200) {
			throw new RuntimeException("Flex report request failed with status: " + response.statusCode());
		}
		return parseFlexResponse(response.body());
	}

	private String parseFlexResponse(String responseBody) {
		try {
			var factory = DocumentBuilderFactory.newInstance();
			var builder = factory.newDocumentBuilder();
			var doc = builder.parse(new java.io.ByteArrayInputStream(responseBody.getBytes()));
			var root = doc.getDocumentElement();
			var statusNodes = root.getElementsByTagName("Status");
			if (statusNodes.getLength() > 0) {
				var status = statusNodes.item(0).getTextContent();
				if (!"Success".equals(status)) {
					throw new RuntimeException("Flex report generation failed with status: " + status);
				}
			}
			
			var referenceNodes = root.getElementsByTagName("ReferenceCode");
			var referenceCode = referenceNodes.item(0).getTextContent();
			log.info("Flex report reference code: {}", referenceCode);
			return referenceCode;
		} catch (Exception e) {
			throw new RuntimeException("Error parsing Flex response");
		}
	}

	private Map<String, Execution> downloadAndParseFlexReport(String referenceCode, Market market) throws IOException, InterruptedException {
		log.info("Downloading Flex report with reference code: {}", referenceCode);
		var requestUrl = String.format("%s/GetStatement?t=%s&q=%s&v=3", flexBaseUrl, reportQueryToken, referenceCode);
		int maxRetries = 5, retryDelaySeconds = 5;
		
		for (int attempt = 1; attempt <= maxRetries; attempt++) {
			log.info("Attempt {} of {} to download Flex report", attempt, maxRetries);
			
			var request = HttpRequest.newBuilder().uri(URI.create(requestUrl)).GET().build();
			var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			
			if (!isReportReady(response.body())) {
				log.info("Flex report not ready yet, attempt {}/{}", attempt, maxRetries);
				Thread.sleep(retryDelaySeconds * 1000);
				continue;
			}

			log.info("Flex report is ready, parsing executions");
			return parseFlexReportCsv(response.body(), market);
		}
		
		log.error("Flex report still not ready after {} attempts", maxRetries);
		return Collections.emptyMap();
	}

	private boolean isReportReady(String responseBody) {
		try {
			if (responseBody.contains("not ready") || 
				responseBody.contains("still processing") ||
				responseBody.contains("generating") ||
				responseBody.contains("pending") ||
				responseBody.contains("Error")) {
				log.debug("Flex report not ready: {}", responseBody);
				return false;
			}

			var lines = responseBody.split("\n");
			if (lines.length < 2) {
				log.debug("Flex report appears empty or incomplete");
				return false;
			}

			var headerLine = lines[0];
			return headerLine.contains("ClientAccountID") &&
				headerLine.contains("Symbol") && 
				headerLine.contains("Buy/Sell") &&
				headerLine.contains("Quantity") &&
				headerLine.contains("Price");
		} catch (Exception e) {
			log.warn("Error checking if report is ready: {}", e.getMessage());
			return false;
		}
	}


	private Map<String, Execution> parseFlexReportCsv(String csvContent, Market market) {
		 var executions = new HashMap<String, Execution>();
		
		try {
			var lines = csvContent.split("\n");
			var headers = parseCsvLine(lines[0]);
			var symbolIndex = findColumnIndex(headers, "Symbol");
			var buySellIndex = findColumnIndex(headers, "Buy/Sell");
			var quantityIndex = findColumnIndex(headers, "Quantity");
			var priceIndex = findColumnIndex(headers, "Price");
			var orderIdIndex = findColumnIndex(headers, "OrderID");
			var execIdIndex = findColumnIndex(headers, "ExecID");
			var dateTimeIndex = findColumnIndex(headers, "Date/Time");
			var commissionIndex = findColumnIndex(headers, "Commission");
			var assetClassIndex = findColumnIndex(headers, "AssetClass");

			if (symbolIndex == -1 || buySellIndex == -1 || quantityIndex == -1 || priceIndex == -1) {
				log.error("Required columns not found in Flex report CSV");
				return Collections.emptyMap();
			}

			for (var i = 1; i < lines.length; i++) {
				var line = lines[i].trim();
				if (line.isEmpty()) continue;
				
				var values = parseCsvLine(line);
				
				var execution = parseExecutionFromCsv(values, market,
					symbolIndex, buySellIndex, quantityIndex, priceIndex, orderIdIndex, execIdIndex, dateTimeIndex,
					commissionIndex, assetClassIndex);
				
				if (execution != null) {
					executions.merge(execution.getOrderId(), execution, Execution::addExecution);
				}
			}
			
			log.info("Parsed {} executions from Flex report CSV", executions.size());
			return executions;
			
		} catch (Exception e) {
			log.error("Error parsing Flex report CSV", e);
			return Collections.emptyMap();
		}
	}

	private String[] parseCsvLine(String line) {
		var result = new ArrayList<String>();
		var inQuotes = false;
		var currentField = new StringBuilder();
		
		for (var i = 0; i < line.length(); i++) {
			var c = line.charAt(i);
			
			if (c == '"') {
				if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
					currentField.append('"');
					i++;
				} else {
					inQuotes = !inQuotes;
				}
			} else if (c == ',' && !inQuotes) {
				result.add(currentField.toString());
				currentField = new StringBuilder();
			} else {
				currentField.append(c);
			}
		}
		
		result.add(currentField.toString());
		return result.toArray(new String[0]);
	}

	private int findColumnIndex(String[] headers, String columnName) {
		for (var i = 0; i < headers.length; i++) {
			if (headers[i].equals(columnName)) {
				return i;
			}
		}
		return -1;
	}

	private Execution parseExecutionFromCsv(String[] values, Market market,
											int symbolIndex, int buySellIndex, int quantityIndex, int priceIndex,
											int orderIdIndex, int execIdIndex, int dateTimeIndex, 
											int commissionIndex, int assetClassIndex) {
		try {
			var execution = new Execution();
			
			var symbol = values[symbolIndex].trim();
			var buySell = values[buySellIndex].trim();
			var quantityStr = values[quantityIndex].trim();
			var priceStr = values[priceIndex].trim();
			
			if (symbol.isEmpty() || buySell.isEmpty() || quantityStr.isEmpty() || priceStr.isEmpty()) {
				return null;
			}
			
			execution.setCode(symbol + "." + market);
			execution.setSide("BUY".equals(buySell) ? BUY : SELL);
			execution.setQuantity(new BigDecimal(quantityStr));
			execution.setPrice(new BigDecimal(priceStr));

			// Set order ID and execution ID
			execution.setOrderId(orderIdIndex != -1 && orderIdIndex < values.length ? values[orderIdIndex].trim() : "UNKNOWN");
			execution.setFillIds(execIdIndex != -1 && execIdIndex < values.length ? values[execIdIndex].trim() : "");

			// Set commission
			if (commissionIndex != -1 && commissionIndex < values.length && !values[commissionIndex].trim().isEmpty()) {
				try {
					execution.setCommission(new BigDecimal(values[commissionIndex].trim()));
				} catch (NumberFormatException e) {
					log.warn("Failed to parse commission: {}", values[commissionIndex].trim());
					execution.setCommission(BigDecimal.ZERO);
				}
			} else {
				execution.setCommission(BigDecimal.ZERO);
			}

			// Set asset class
			execution.setAssetClass(assetClassIndex != -1 && assetClassIndex < values.length ? values[assetClassIndex].trim() : "");

			execution.setMarket(market);

			// Parse execution time
			try {
				if (dateTimeIndex != -1 && dateTimeIndex < values.length) {
					var flexDateFormat = new SimpleDateFormat("yyyyMMdd HH:mm:ss z");
					execution.setTime(flexDateFormat.parse(values[dateTimeIndex].trim()).getTime());
				} else {
					execution.setTime(System.currentTimeMillis());
				}
			} catch (Exception e) {
				execution.setTime(System.currentTimeMillis());
			}
			
			return execution;
			
		} catch (Exception e) {
			log.error("Error parsing execution from CSV row", e);
			return null;
		}
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
	public void orderStatus(int orderId, String status, Decimal filled, Decimal remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld, double mktCapPrice) {
		log.info("Order status update: orderId={}, status={}, filled={}, remaining={}", orderId, status, filled, remaining);
		orderStatusDetails.put(orderId, new OrderStatusDetail(orderId, status, filled, remaining, avgFillPrice));
	}

	record OrderStatusDetail(int orderId, String status, Decimal filled, Decimal remaining, double avgFillPrice) {}

	record OrderDetail(int orderId, Contract contract, com.ib.client.Order order, OrderState orderState, Date updatedAt) {
		@NotNull
		@Override
		public String toString() {
			return "%s:%s:%s@%s (state=%s, execQty=%s)".formatted(order.action(), contract.symbol(), order.totalQuantity(), order.lmtPrice(),
					orderState.status(), order.filledQuantity());
		}
	}

	record ExecDetail(Contract contract, com.ib.client.Execution execution) {}

	@Override
	public void openOrder(int orderId, Contract contract, com.ib.client.Order order, OrderState orderState) {
		log.info("Received open order: orderId={}, symbol={}, action={}, qty={}",
			orderId, contract.symbol(), order.action(), order.totalQuantity());
		orderDetails.put(orderId, new OrderDetail(orderId, contract, order, orderState, new Date()));
	}

	@Override
	public void openOrderEnd() {
		log.info("Open orders request completed");
		completeOperation(Operation.GET_PENDING_ORDERS);
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
		executions.put(execution.execId(), new ExecDetail(contract, execution));
	}

	@Override
	public void execDetailsEnd(int reqId) {
        log.info("execDetailsEnd: {}", reqId);
		completeOperation(Operation.GET_EXECUTIONS);
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

	private Contract createContract(String code) {
		Market market = Market.getMarketByStockCode(code);
		String symbol = Market.getBaseCodeFromTicker(code);
		
		Contract contract = new Contract();
		contract.symbol(symbol);
		contract.secType("STK");
		contract.exchange("SMART");
		
		switch (market) {
			case US:
				contract.currency("USD");
				break;
			case HK:
				contract.currency("HKD");
				break;
		}
		
		return contract;
	}
	
	private com.ib.client.Order createIBOrder(SquoteConstants.Side side, int quantity, double price) {
		com.ib.client.Order order = new com.ib.client.Order();
		order.action(side == SquoteConstants.Side.BUY ? "BUY" : "SELL");
		order.totalQuantity(Decimal.get(quantity));
		order.orderType("LMT");
		order.lmtPrice(price);
		order.tif("GTC");
		order.outsideRth(true);
		return order;
	}
	
	private Order toOrder(OrderDetail orderDetail, Market market) {
		try {
			var side = orderDetail.order.action() == Types.Action.BUY ? SquoteConstants.Side.BUY : SquoteConstants.Side.SELL;
			var execDetail = orderStatusDetails.get(orderDetail.orderId());
			double fillAvgPrice = execDetail == null ? 0 : execDetail.avgFillPrice;
			return new Order(orderDetail.contract.symbol() + "." + market, side,
					orderDetail.order.totalQuantity().value().intValue(),
					orderDetail.order.lmtPrice(), orderDetail.orderId,
					orderDetail.order.filledQuantity().value().intValue(),
					fillAvgPrice, orderDetail.updatedAt);
		} catch (Exception e) {
			log.error("Error converting IB order to Order object", e);
			return null;
		}
	}
}
    
