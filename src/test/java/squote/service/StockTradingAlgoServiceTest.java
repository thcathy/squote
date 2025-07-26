package squote.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.mashape.unirest.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import squote.domain.*;
import squote.domain.repository.DailyAssetSummaryRepository;
import squote.domain.repository.FundRepository;
import squote.domain.repository.HoldingStockRepository;
import squote.scheduletask.FutuClientConfig;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;
import static squote.SquoteConstants.Side.BUY;
import static squote.SquoteConstants.Side.SELL;

class StockTradingAlgoServiceTest {
    DailyAssetSummaryRepository dailyAssetSummaryRepo = Mockito.mock(DailyAssetSummaryRepository.class);
    FundRepository mockFundRepo = Mockito.mock(FundRepository.class);
    HoldingStockRepository holdingStockRepository = Mockito.mock(HoldingStockRepository.class);
    IBrokerAPIClient mockBrokerAPIClient = Mockito.mock(IBrokerAPIClient.class);
    TelegramAPIClient mockTelegramAPIClient = Mockito.mock(TelegramAPIClient.class);
    WebParserRestService mockWebParserRestService = Mockito.mock(WebParserRestService.class);

    private StockTradingAlgoService stockTradingAlgoService;
    private ListAppender<ILoggingEvent> listAppender;

    private static final String stockCode = "code1";
    private static final String stockCodeUS = "AAPL.US";
    int stdDevRange = 20;
    double stdDev = 1.35;
    double stdDevMultiplier = 0.95;
    Fund fundA = new Fund("UserA", "FundA");
    Fund fundB = new Fund("UserA", "FundB");
    Fund fundUS = new Fund("UserA", "FundUS");

    @BeforeEach
    void setUp() {
        listAppender = new ListAppender<>();
        listAppender.setName("testAppender");
        listAppender.start();

        Logger logger = (Logger) LoggerFactory.getLogger(StockTradingAlgoService.class);
        logger.addAppender(listAppender);

        var summary = new DailyAssetSummary(stockCode, new Date());
        summary.stdDevs.put(stdDevRange, stdDev);
        when(dailyAssetSummaryRepo.findTopBySymbolOrderByDateDesc(any())).thenReturn(Optional.of(summary));

        when(mockBrokerAPIClient.placeOrder(any(), any(), anyInt(), anyDouble())).thenReturn(new FutuAPIClient.PlaceOrderResponse(1L, 0, null));
        when(mockBrokerAPIClient.cancelOrder(anyLong(), anyString()))
                .thenReturn(new FutuAPIClient.CancelOrderResponse(0, ""));
        when(mockTelegramAPIClient.sendMessage(any())).thenReturn(List.of("Message sent"));

        // setup stock quote
        var quote = new StockQuote(stockCode);
        quote.setPrice("40");   // high price default to pass most of the cases
        when(mockBrokerAPIClient.getStockQuote(any())).thenReturn(quote);

        var stockQuoteUS = new StockQuote(stockCodeUS);
        stockQuoteUS.setPrice("200.0");
        HttpResponse<StockQuote[]> mockHttpResponse = Mockito.mock(HttpResponse.class);
        when(mockHttpResponse.getBody()).thenReturn(new StockQuote[]{stockQuoteUS});
        var mockFuture = CompletableFuture.completedFuture(mockHttpResponse);
        when(mockWebParserRestService.getRealTimeQuotes(List.of(stockCodeUS))).thenReturn(mockFuture);

        // setup fund
        var algoConfig = new AlgoConfig(stockCode, 3500, (Double) null, stdDevRange, stdDevMultiplier, null);
        fundA.getAlgoConfigs().put(stockCode, algoConfig);
        fundB.getAlgoConfigs().put(stockCode, algoConfig);
        var usAlgoConfig = new AlgoConfig(stockCodeUS, 100, (Double) null, stdDevRange, stdDevMultiplier, null);
        fundUS.getAlgoConfigs().put(stockCodeUS, usAlgoConfig);
        when(mockFundRepo.findAll()).thenReturn(Arrays.asList(fundA, fundB, fundUS));

        stockTradingAlgoService = new StockTradingAlgoService(
                dailyAssetSummaryRepo, mockFundRepo, holdingStockRepository, mockWebParserRestService, mockTelegramAPIClient);
    }

    private AlgoConfig getDefaultAlgoConfig() {
        return getAlgoConfigWithQuantity(3500);
    }

    private AlgoConfig getAlgoConfigWithQuantity(int quantity) {
        return new AlgoConfig(stockCode, quantity, (Double) null, stdDevRange, stdDevMultiplier, null);
    }

    static Stream<TestFindBasePriceData> testFindBasePriceDataProvider() {
        return Stream.of(
                // single buy, pick that one
                new TestFindBasePriceData(List.of(
                        HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA", new Date())
                ), "base price: 20.0"),
                // multi buy, pick the lowest price
                new TestFindBasePriceData(List.of(
                        HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA", new Date()),
                        HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(88000), "FundA", new Date())
                ), "base price: 20.0"),
                // no buy, 1 sell, pick sell price
                new TestFindBasePriceData(List.of(
                        HoldingStock.simple(stockCode, SELL, 4000, BigDecimal.valueOf(80000), "FundA", new Date())
                ), "base price: 20.0"),
                // 1 buy, 1 sell after buy, pick sell price
                new TestFindBasePriceData(List.of(
                        HoldingStock.simple(stockCode, SELL, 4000, BigDecimal.valueOf(88000), "FundA", new Date(1736308300000L)),
                        HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA", new Date(1736308200000L))
                ), "base price: 22.0"),
                // n buy, 1 sell, pick n-1 buy price
                new TestFindBasePriceData(List.of(
                        HoldingStock.simple(stockCode, SELL, 4000, BigDecimal.valueOf(88000), "FundA", new Date(1736308300000L)),
                        HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA", new Date(1736308200000L)),
                        HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(88000), "FundA", new Date(1736308210000L))
                ), "base price: 22.0"),
                // n buy, m sell, pick n-m buy price
                new TestFindBasePriceData(List.of(
                        HoldingStock.simple(stockCode, SELL, 4000, BigDecimal.valueOf(88000), "FundA", new Date(1736308450000L)),
                        HoldingStock.simple(stockCode, SELL, 4000, BigDecimal.valueOf(98000), "FundA", new Date(1736308350000L)),
                        HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA", new Date(1736308400000L)),
                        HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(90000), "FundA", new Date(1736308300000L)),
                        HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(100000), "FundA", new Date(1736308200000L))
                ), "base price: 25.0")
        );
    }

    record TestFindBasePriceData(List<HoldingStock> holdings, String expectedLog) {}

    @ParameterizedTest
    @MethodSource("testFindBasePriceDataProvider")
    void testFindBasePrice(TestFindBasePriceData testData) {
        when(holdingStockRepository.findByUserIdOrderByDate("UserA"))
            .thenReturn(testData.holdings);
        stockTradingAlgoService.processSingleSymbol(
                fundA, Market.HK,
                getDefaultAlgoConfig(),
                FutuClientConfig.defaultConfig(),
                mockBrokerAPIClient
        );
        var logMatched = listAppender.list.stream().anyMatch(l -> l.getFormattedMessage().startsWith(testData.expectedLog));
        assertThat(logMatched).isTrue();
    }

    @Test
    void testFindBasePriceErrorCase() {
        when(holdingStockRepository.findByUserIdOrderByDate("UserA"))
                .thenReturn(List.of(
                        HoldingStock.simple(stockCode, SELL, 4000, BigDecimal.valueOf(80000), "FundA", new Date(1736308300000L)),
                        HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(88000), "FundA", new Date(1736308260000L))
                ));
        stockTradingAlgoService.processSingleSymbol(
                fundA, Market.HK,
                getDefaultAlgoConfig(),
                FutuClientConfig.defaultConfig(),
                mockBrokerAPIClient
        );
        var logMatched = listAppender.list.stream().anyMatch(
                l -> l.getFormattedMessage().matches("Unexpected executions: SELL code1 4000@20.00 .* 2025 < BUY code1 4000@22.00 .* 2025"));
        assertThat(logMatched).isTrue();
    }

    @Test
    void testExecutionToStringMethod() {
        StockTradingAlgoService.Execution execution = new StockTradingAlgoService.Execution("GOOG", SELL, 50, 123.45, false, new Date(1736308260000L));
        String expectedString = "SELL GOOG 50@123.45 (isToday=false) ";

        assertThat(execution.toString()).startsWith(expectedString);
    }

    @Test
    void noPendingBuyOrder_willPlaceOrder() {
        var executionQuantity = 4000;
        var algoConfigQuantity = 2500;
        var holding = HoldingStock.simple(stockCode, BUY, executionQuantity, BigDecimal.valueOf(80000), "FundA");
        var algoConfig = getAlgoConfigWithQuantity(algoConfigQuantity);
        var expectedPrice = 19.74; // 20.0 / (1 + (stdDev * stdDevMultiplier / 100));
        
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));
        when(mockBrokerAPIClient.getPendingOrders(Market.HK)).thenReturn(List.of());

        stockTradingAlgoService.processSingleSymbol(
                fundA, Market.HK,
                algoConfig,
                FutuClientConfig.defaultConfig(),
                mockBrokerAPIClient
        );

        verify(mockBrokerAPIClient, times(1)).placeOrder(eq(BUY), eq(stockCode), eq(algoConfigQuantity), eq(expectedPrice));
        verify(mockTelegramAPIClient, times(1)).sendMessage(startsWith("Placed order (FundA): BUY"));
    }

    @Test
    void buyOrderPrice_mustLowerThanMarketPrice() {
        var holding = HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA");
        var marketPrice = 19;
        var maxBuyPrice = 18.88; // base / (stdDev * 1.5)
        var quote = new StockQuote(stockCode);

        quote.setPrice(String.valueOf(marketPrice));
        when(mockBrokerAPIClient.getStockQuote(any())).thenReturn(quote);
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));
        when(mockBrokerAPIClient.getPendingOrders(Market.HK)).thenReturn(List.of());

        stockTradingAlgoService.processSingleSymbol(
                fundA, Market.HK,
                getDefaultAlgoConfig(),
                FutuClientConfig.defaultConfig(),
                mockBrokerAPIClient
        );

        verify(mockBrokerAPIClient, times(1)).placeOrder(eq(BUY), eq(stockCode), eq(3500), eq(maxBuyPrice));
        verify(mockTelegramAPIClient, times(1)).sendMessage(startsWith("Placed order (FundA): BUY"));
    }


    @Test
    void pendingBuyOrderPriceOverThreshold_replaceOrder() {
        var holding = HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA");
        var expectedPrice = 19.74; // 20.0 / (1 + (stdDev * stdDevMultiplier / 100));
        var pendingOrderId = 123456L;
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));
        when(mockBrokerAPIClient.getPendingOrders(Market.HK)).thenReturn(List.of(
                Order.newOrder(stockCode, BUY, 4000, expectedPrice * 1.0021, pendingOrderId)
        ));

        stockTradingAlgoService.processSingleSymbol(
                fundA, Market.HK,
                getDefaultAlgoConfig(),
                FutuClientConfig.defaultConfig(),
                mockBrokerAPIClient
        );

        verify(mockBrokerAPIClient, times(1)).cancelOrder(eq(pendingOrderId), eq(stockCode));
        verify(mockTelegramAPIClient, times(1)).sendMessage(startsWith("Cancelled order (FundA): BUY"));
        verify(mockBrokerAPIClient, times(1)).placeOrder(eq(BUY), eq(stockCode), eq(3500), eq(expectedPrice));
        verify(mockTelegramAPIClient, times(1)).sendMessage(startsWith("Placed order (FundA): BUY"));
    }

    @Test
    void pendingBuyOrderPriceWithinThreshold_dontPlaceOrder() {
        var holding = HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA");
        var expectedPrice = 19.74; // 20.0 / (1 + (stdDev * stdDevMultiplier / 100));
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));
        when(mockBrokerAPIClient.getPendingOrders(Market.HK)).thenReturn(List.of(
                Order.newOrder(stockCode, BUY, 4000, expectedPrice * 1.0002, 123456L) ,  // price within threshold
                Order.newOrder("DifferentStock", BUY, 4000, expectedPrice * 1.0002, 123456L)
        ));

        stockTradingAlgoService.processSingleSymbol(
                fundA, Market.HK,
                getDefaultAlgoConfig(),
                FutuClientConfig.defaultConfig(),
                mockBrokerAPIClient
        );

        verify(mockBrokerAPIClient, never()).placeOrder(eq(BUY), any(), anyInt(), anyDouble());
        verify(mockTelegramAPIClient, never()).sendMessage(contains("placed order: BUY"));
    }

    @Test
    void multiPendingBuyOrders_cancelAllThenNew() {
        var holding = HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA");
        var expectedPrice = 19.74; // 20.0 / (1 + (stdDev * stdDevMultiplier / 100));
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));
        when(mockBrokerAPIClient.getPendingOrders(Market.HK)).thenReturn(List.of(
                Order.newOrder(stockCode, BUY, 4000, expectedPrice / 1.0002, 123456L),   // price within threshold,
                Order.newOrder(stockCode, BUY, 3500, expectedPrice / 1.1, 123456L)   // price within threshold
        ));

        stockTradingAlgoService.processSingleSymbol(
                fundA, Market.HK,
                getDefaultAlgoConfig(),
                FutuClientConfig.defaultConfig(),
                mockBrokerAPIClient
        );

        verify(mockBrokerAPIClient, times(2)).cancelOrder(anyLong(), anyString());
        verify(mockTelegramAPIClient, times(2)).sendMessage(startsWith("Cancelled order due to multiple pending (FundA): BUY"));
        verify(mockBrokerAPIClient, times(1)).placeOrder(eq(BUY), eq(stockCode), eq(3500), eq(expectedPrice));
        verify(mockTelegramAPIClient, times(1)).sendMessage(startsWith("Placed order (FundA): BUY"));
    }

    @Test
    void cancelOrderFailed_throwException() {
        var holding = HoldingStock.simple(stockCode, SELL, 4000, BigDecimal.valueOf(80000), "FundA");
        var expectedPrice = 19.72; // 20.0 / (1 + (stdDev * stdDevMultiplier / 100));
        var pendingOrderId = 123456L;
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));
        when(mockBrokerAPIClient.getPendingOrders(Market.HK)).thenReturn(List.of(
                Order.newOrder(stockCode, BUY, 4000, expectedPrice * 1.0021, pendingOrderId)
        ));
        when(mockBrokerAPIClient.cancelOrder(anyLong(), anyString()))
                .thenReturn(new FutuAPIClient.CancelOrderResponse(400, "error for testing"));

        assertThrows(RuntimeException.class, () ->
                stockTradingAlgoService.processSingleSymbol(
                fundA, Market.HK,
                getDefaultAlgoConfig(),
                FutuClientConfig.defaultConfig(),
                mockBrokerAPIClient
        ));
    }

    @Test
    void baseIsSellOrder_dontPlaceSellOrder() {
        var holding = HoldingStock.simple(stockCode, SELL, 4000, BigDecimal.valueOf(80000), "FundA");
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));

        stockTradingAlgoService.processSingleSymbol(
                fundA, Market.HK,
                getDefaultAlgoConfig(),
                FutuClientConfig.defaultConfig(),
                mockBrokerAPIClient
        );

        verify(mockBrokerAPIClient, never()).placeOrder(eq(SELL), any(), anyInt(), anyDouble());
    }

    @Test
    void baseIsSell_priceLargerThanMinBuyPrice() {
        var holding = HoldingStock.simple(stockCode, SELL, 4000, BigDecimal.valueOf(80000), "FundA");
        var marketPrice = 22;
        var expectedPrice = 21.72; // nearest to (mkt price - 1 stdDev)
        var quote = new StockQuote(stockCode);

        quote.setPrice(String.valueOf(marketPrice));
        when(mockBrokerAPIClient.getStockQuote(any())).thenReturn(quote);
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));

        stockTradingAlgoService.processSingleSymbol(
                fundA, Market.HK,
                getDefaultAlgoConfig(),
                FutuClientConfig.defaultConfig(),
                mockBrokerAPIClient
        );

        verify(mockBrokerAPIClient, times(1)).placeOrder(eq(BUY), eq(stockCode), eq(3500), eq(expectedPrice));
    }

    @Test
    void baseIsSell_targetPriceLessThanPendingPrice_doNothing() {
        var holding = HoldingStock.simple(stockCode, SELL, 4000, BigDecimal.valueOf(80000), "FundA");
        var marketPrice = 22;
        var targetPrice = 21.72; // nearest to (mkt price - 1 stdDev)
        var quote = new StockQuote(stockCode);

        quote.setPrice(String.valueOf(marketPrice));
        when(mockBrokerAPIClient.getStockQuote(any())).thenReturn(quote);
        when(mockBrokerAPIClient.getPendingOrders(Market.HK)).thenReturn(List.of(
                Order.newOrder(stockCode, BUY, 4000, targetPrice + 0.02, 123456L)
        ));
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));

        stockTradingAlgoService.processSingleSymbol(
                fundA, Market.HK,
                getDefaultAlgoConfig(),
                FutuClientConfig.defaultConfig(),
                mockBrokerAPIClient
        );

        verify(mockBrokerAPIClient, never()).placeOrder(eq(BUY), any(), anyInt(), anyDouble());
    }

    @Test
    void noPendingSellOrder_placeOrder() {
        var executionQuantity = 3000;
        var algoConfigQuantity = 1500;
        var holding = HoldingStock.simple(stockCode, BUY, executionQuantity, BigDecimal.valueOf(60000), "FundA");
        var algoConfig = getAlgoConfigWithQuantity(algoConfigQuantity);
        var expectedPrice = 20.26; // 20.0 * (1 + (stdDev * stdDevMultiplier / 100));
        
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));
        when(mockBrokerAPIClient.getPendingOrders(Market.HK)).thenReturn(List.of());

        stockTradingAlgoService.processSingleSymbol(
                fundA, Market.HK,
                algoConfig,
                FutuClientConfig.defaultConfig(),
                mockBrokerAPIClient
        );

        verify(mockBrokerAPIClient, times(1)).placeOrder(eq(SELL), eq(stockCode), eq(executionQuantity), eq(expectedPrice));
    }

    @Test
    void pendingSellPriceWithinThreshold_doNothing() {
        var holding = HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA");
        var expectedPrice = 20.26; // 20.0 * (1 + (stdDev * stdDevMultiplier / 100));
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));
        when(mockBrokerAPIClient.getPendingOrders(Market.HK)).thenReturn(List.of(
                Order.newOrder(stockCode, SELL, 4000, expectedPrice / 1.0001, 123456L)
        ));

        stockTradingAlgoService.processSingleSymbol(
                fundA, Market.HK,
                getDefaultAlgoConfig(),
                FutuClientConfig.defaultConfig(),
                mockBrokerAPIClient
        );

        verify(mockBrokerAPIClient, never()).placeOrder(eq(SELL), any(), anyInt(), anyDouble());
    }

    @Test
    void pendingSellPriceOverThreshold_replaceSellOrder() {
        var holding = HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA");
        var expectedPrice = 20.26; // 20.0 * (1 + (stdDev * stdDevMultiplier / 100));
        var pendingOrderId = 123456L;
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));
        when(mockBrokerAPIClient.getPendingOrders(Market.HK)).thenReturn(List.of(
                Order.newOrder(stockCode, BUY, 4000, expectedPrice * 1.0021, pendingOrderId)
        ));

        stockTradingAlgoService.processSingleSymbol(
                fundA, Market.HK,
                getDefaultAlgoConfig(),
                FutuClientConfig.defaultConfig(),
                mockBrokerAPIClient
        );

        verify(mockBrokerAPIClient, times(1)).cancelOrder(eq(pendingOrderId), eq(stockCode));
        verify(mockTelegramAPIClient, times(1)).sendMessage(startsWith("Cancelled order (FundA): BUY"));
        verify(mockBrokerAPIClient, times(1)).placeOrder(eq(SELL), eq(stockCode), eq(4000), eq(expectedPrice));
    }

    @Test
    void hasPartialFill_doNothing() {
        var holding = HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA");
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));
        when(mockBrokerAPIClient.getPendingOrders(Market.HK)).thenReturn(List.of(
                new Order(stockCode, BUY, 4000, 20.5, 123456L, 1000, 20.48, new Date()),
                Order.newOrder(stockCode, SELL, 4000, 19.5, 123456L)
        ));

        stockTradingAlgoService.processSingleSymbol(
                fundA, Market.HK,
                getDefaultAlgoConfig(),
                FutuClientConfig.defaultConfig(),
                mockBrokerAPIClient
        );

        verify(mockBrokerAPIClient, never()).placeOrder(any(), any(), anyInt(), anyDouble());
        var logMatched = listAppender.list.stream().anyMatch(
                l -> l.getFormattedMessage().startsWith("Has partial filled pending order"));
        assertThat(logMatched).isTrue();
    }

    @Test
    void testFindBasePriceWithTDayExecutions() {
        List<HoldingStock> holdings = List.of(
                HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(77920), "FundA", new Date(1736233800000L)),
                HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(78800), "FundA", new Date(1736230200000L)));
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(holdings);
        var sellExecutionAtT1 = new Execution();
        sellExecutionAtT1.setCode(stockCode);
        sellExecutionAtT1.setSide(SELL);
        sellExecutionAtT1.setOrderId("sell order 1");
        sellExecutionAtT1.setPrice(new BigDecimal("19.68"));
        sellExecutionAtT1.setQuantity(new BigDecimal("4000"));
        sellExecutionAtT1.setTime(1736300520000L);
        var buyExecutionAtT1 = new Execution();
        buyExecutionAtT1.setSide(BUY);
        buyExecutionAtT1.setCode(stockCode);
        buyExecutionAtT1.setOrderId("buy order 1");
        buyExecutionAtT1.setPrice(new BigDecimal("19.3"));
        buyExecutionAtT1.setQuantity(new BigDecimal("4000"));
        buyExecutionAtT1.setTime(1736308260000L);
        var buyExecutionAtT2 = new Execution();
        buyExecutionAtT2.setSide(BUY);
        buyExecutionAtT2.setCode(stockCode);
        buyExecutionAtT2.setOrderId("buy order 1");
        buyExecutionAtT2.setPrice(new BigDecimal("19.5"));
        buyExecutionAtT2.setQuantity(new BigDecimal("4000"));
        buyExecutionAtT2.setTime(1736321400000L);

        var TdayExecutions = new HashMap<>(Map.ofEntries(
                Map.entry("buy1", buyExecutionAtT1),
                Map.entry("buy2", buyExecutionAtT2),
                Map.entry("sell1", sellExecutionAtT1)
        ));
        when(mockBrokerAPIClient.getStockTodayExecutions(Market.HK)).thenReturn(TdayExecutions);
        stockTradingAlgoService.processSingleSymbol(
                fundA, Market.HK,
                getDefaultAlgoConfig(),
                FutuClientConfig.defaultConfig(),
                mockBrokerAPIClient
        );
        var logMatched = listAppender.list.stream().anyMatch(l -> l.getFormattedMessage().startsWith("base price: 19.3"));
        assertThat(logMatched).isTrue();

        // one more sell at T
        var sellExecutionAtT2 = new Execution();
        sellExecutionAtT2.setCode(stockCode);
        sellExecutionAtT2.setSide(SELL);
        sellExecutionAtT2.setOrderId("sell order 2");
        sellExecutionAtT2.setPrice(new BigDecimal("19.5"));
        sellExecutionAtT2.setQuantity(new BigDecimal("4000"));
        sellExecutionAtT2.setTime(1736321700000L);
        TdayExecutions.put("sell2", sellExecutionAtT2);

        stockTradingAlgoService.processSingleSymbol(
                fundA, Market.HK,
                getDefaultAlgoConfig(),
                FutuClientConfig.defaultConfig(),
                mockBrokerAPIClient
        );
        var logMatched2 = listAppender.list.stream().anyMatch(l -> l.getFormattedMessage().startsWith("base price: 19.5"));
        assertThat(logMatched2).isTrue();
    }

    @Test
    void partialFilledOrder_willSendWarningMessageToTelegram() {
        var holding = HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA");
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));
        var partialFilledOrder = new Order(stockCode, BUY, 4000, 20.5, 123456L, 3500, 20.48, new Date());
        when(mockBrokerAPIClient.getPendingOrders(Market.HK)).thenReturn(List.of(partialFilledOrder));

        stockTradingAlgoService.processSingleSymbol(
                fundA, Market.HK,
                getDefaultAlgoConfig(),
                FutuClientConfig.defaultConfig(),
                mockBrokerAPIClient
        );

        verify(mockTelegramAPIClient, times(1)).sendMessage(contains("WARN: Partial filled BUY 4000@20.50. filled=3500"));
    }

    @Test
    void differentQuantitiesForBuyAndSell_usesCorrectQuantityForEachSide() {
        // Setup: execution quantity = 2000, AlgoConfig quantity = 1000
        var executionQuantity = 2000;
        var algoConfigQuantity = 1000;
        var holding = HoldingStock.simple(stockCode, BUY, executionQuantity, BigDecimal.valueOf(40000), "FundA");
        var algoConfig = getAlgoConfigWithQuantity(algoConfigQuantity);
        
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));
        when(mockBrokerAPIClient.getPendingOrders(Market.HK)).thenReturn(List.of());

        stockTradingAlgoService.processSingleSymbol(
                fundA, Market.HK,
                algoConfig,
                FutuClientConfig.defaultConfig(),
                mockBrokerAPIClient
        );

        verify(mockBrokerAPIClient, times(1)).placeOrder(eq(BUY), eq(stockCode), eq(algoConfigQuantity), anyDouble());
        verify(mockBrokerAPIClient, times(1)).placeOrder(eq(SELL), eq(stockCode), eq(executionQuantity), anyDouble());
    }

    @Test
    void algoConfigQuantityZero_buyOrderUsesExecutionQuantity() {
        var executionQuantity = 2500;
        var algoConfigQuantity = 0;
        var holding = HoldingStock.simple(stockCode, BUY, executionQuantity, BigDecimal.valueOf(50000), "FundA");
        var algoConfig = getAlgoConfigWithQuantity(algoConfigQuantity);
        var expectedPrice = 19.74; // 20.0 / (1 + (stdDev * stdDevMultiplier / 100));
        
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));
        when(mockBrokerAPIClient.getPendingOrders(Market.HK)).thenReturn(List.of());

        stockTradingAlgoService.processSingleSymbol(
                fundA, Market.HK,
                algoConfig,
                FutuClientConfig.defaultConfig(),
                mockBrokerAPIClient
        );

        verify(mockBrokerAPIClient, times(1)).placeOrder(eq(BUY), eq(stockCode), eq(executionQuantity), eq(expectedPrice));
        verify(mockBrokerAPIClient, times(1)).placeOrder(eq(SELL), eq(stockCode), eq(executionQuantity), anyDouble());
        verify(mockTelegramAPIClient, times(1)).sendMessage(startsWith("Placed order (FundA): BUY"));
        verify(mockTelegramAPIClient, times(1)).sendMessage(startsWith("Placed order (FundA): SELL"));
    }

    @Test
    void processSingleSymbol_testUSStockTrading() {
        var usHolding = HoldingStock.simple(stockCodeUS, BUY, 10, BigDecimal.valueOf(2000), "FundA");
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(usHolding));

        when(mockBrokerAPIClient.getPendingOrders(Market.US)).thenReturn(List.of(
                Order.newOrder(stockCodeUS, BUY, 10, 199.17, 1L)    // match target price
        ));
        var buyExecutionAtT1 = new Execution();
        buyExecutionAtT1.setSide(BUY);
        buyExecutionAtT1.setCode(stockCodeUS);
        buyExecutionAtT1.setOrderId("buy order 1");
        buyExecutionAtT1.setPrice(new BigDecimal("500"));
        buyExecutionAtT1.setQuantity(new BigDecimal("10"));
        buyExecutionAtT1.setTime(1736308260000L);
        var TdayExecutions = new HashMap<>(Map.ofEntries(
                Map.entry("buy1", buyExecutionAtT1)
        ));
        when(mockBrokerAPIClient.getStockTodayExecutions(Market.US)).thenReturn(TdayExecutions);

        stockTradingAlgoService.processSingleSymbol(
                fundUS, Market.US,
                fundUS.getAlgoConfigs().get(stockCodeUS),
                FutuClientConfig.defaultConfig(),
                mockBrokerAPIClient
        );

        verify(mockWebParserRestService, times(1)).getRealTimeQuotes(List.of(stockCodeUS));
        verify(mockBrokerAPIClient, never()).getStockQuote(stockCodeUS);
        verify(mockBrokerAPIClient, times(1)).getStockTodayExecutions(Market.US);
    }

    @Test
    void processSingleSymbol_shouldCalculateQuantityFromGrossAmount() {
        var grossAmountBasedConfig = new AlgoConfig(stockCode, 0, (Double) null, stdDevRange, stdDevMultiplier, 1000.0);
        fundA.getAlgoConfigs().put(stockCode, grossAmountBasedConfig);
        
        var holding = HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA");
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));
        when(mockBrokerAPIClient.getPendingOrders(Market.HK)).thenReturn(List.of());
        when(mockBrokerAPIClient.getStockTodayExecutions(Market.HK)).thenReturn(new HashMap<>());

        // Act
        stockTradingAlgoService.processSingleSymbol(fundA, Market.HK, grossAmountBasedConfig,
                FutuClientConfig.defaultConfig(), mockBrokerAPIClient);

        verify(mockBrokerAPIClient, times(1)).placeOrder(eq(BUY), eq(stockCode), eq(50), anyDouble());
    }
}
