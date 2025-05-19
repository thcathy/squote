package squote.scheduletask;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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
import squote.service.FutuAPIClient;
import squote.service.TelegramAPIClient;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static squote.SquoteConstants.Side.BUY;
import static squote.SquoteConstants.Side.SELL;

class StockTradingTaskTest {
    DailyAssetSummaryRepository dailyAssetSummaryRepo = Mockito.mock(DailyAssetSummaryRepository.class);
    FundRepository mockFundRepo = Mockito.mock(FundRepository.class);
    HoldingStockRepository holdingStockRepository = Mockito.mock(HoldingStockRepository.class);
    FutuAPIClientFactory mockFactory = Mockito.mock(FutuAPIClientFactory.class);
    FutuAPIClient mockFutuAPIClient = Mockito.mock(FutuAPIClient.class);
    TelegramAPIClient mockTelegramAPIClient = Mockito.mock(TelegramAPIClient.class);

    private StockTradingTask stockTradingTask;
    private ListAppender<ILoggingEvent> listAppender;

    private static final String stockCode = "code1";
    int stdDevRange = 20;
    double stdDev = 1.35;
    double stdDevMultiplier = 0.95;

    @BeforeEach
    void setUp() {
        listAppender = new ListAppender<>();
        listAppender.setName("testAppender");
        listAppender.start();

        Logger logger = (Logger) LoggerFactory.getLogger(StockTradingTask.class);
        logger.addAppender(listAppender);

        when(mockFactory.build(any(), anyShort())).thenReturn(mockFutuAPIClient);

        var summary = new DailyAssetSummary(stockCode, new Date());
        summary.stdDevs.put(stdDevRange, stdDev);
        when(dailyAssetSummaryRepo.findTopBySymbolOrderByDateDesc(any())).thenReturn(Optional.of(summary));

        when(mockFutuAPIClient.placeOrder(anyLong(), any(), any(), anyInt(), anyDouble())).thenReturn(new FutuAPIClient.PlaceOrderResponse(1L, 0, null));
        when(mockFutuAPIClient.cancelOrder(anyLong(), anyLong()))
                .thenReturn(new FutuAPIClient.CancelOrderResponse(0, ""));
        when(mockTelegramAPIClient.sendMessage(any())).thenReturn(List.of("Message sent"));
        when(mockFutuAPIClient.unlockTrade(any())).thenReturn(true);

        var quote = new StockQuote(stockCode);
        quote.setPrice("40");   // high price default to pass most of the cases
        when(mockFutuAPIClient.getStockQuote(any())).thenReturn(quote);

        stockTradingTask = new StockTradingTask(dailyAssetSummaryRepo, mockFundRepo, holdingStockRepository, mockTelegramAPIClient);
        stockTradingTask.futuAPIClientFactory = mockFactory;
        stockTradingTask.enabled = true;  // Enable the task for testing
        stockTradingTask.stdDevRange = stdDevRange;
        stockTradingTask.stdDevMultiplier = stdDevMultiplier;
        stockTradingTask.clientConfigJson = """
                    [
                        {"fundName": "FundA", "fundUserId": "UserA", "accountId": 1, "ip": "192.0.0.1", "port": 80, "unlockCode": "dummy code"},
                        {"fundName": "FundB", "fundUserId": "UserB", "accountId": 2, "ip": "192.0.0.1", "port": 80, "unlockCode": "dummy code"}
                    ]
                """;

        var algoConfig = new AlgoConfig(stockCode, 3500, null);
        var fundA = new Fund("dummy", "FundA");
        fundA.getAlgoConfigs().put(stockCode, algoConfig);
        var fundB = new Fund("dummy", "FundB");
        fundB.getAlgoConfigs().put(stockCode, algoConfig);
        when(mockFundRepo.findAll()).thenReturn(Arrays.asList(fundA, fundB));
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
        stockTradingTask.executeTask();
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
        stockTradingTask.executeTask();
        var logMatched = listAppender.list.stream().anyMatch(
                l -> l.getFormattedMessage().matches("Unexpected executions: SELL code1 4000@20.00 .* 2025 < BUY code1 4000@22.00 .* 2025"));
        assertThat(logMatched).isTrue();
    }

    @Test
    void testExecuteTaskDisabled() {
        stockTradingTask.enabled = false;
        stockTradingTask.executeTask();

        verifyNoInteractions(dailyAssetSummaryRepo);
        verifyNoInteractions(mockFundRepo);
        verifyNoInteractions(holdingStockRepository);
    }

    @Test
    void testExecutionToStringMethod() {
        StockTradingTask.Execution execution = new StockTradingTask.Execution("GOOG", SELL, 50, 123.45, false, new Date(1736308260000L));
        String expectedString = "SELL GOOG 50@123.45 (isToday=false) ";

        assertThat(execution.toString()).startsWith(expectedString);
    }

    @Test
    void executeTask_willCloseFutuConnection() {
        var holding = HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA");
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));

        stockTradingTask.executeTask();

        verify(mockFutuAPIClient, atLeast(1)).close();
    }

    @Test
    void noPendingBuyOrder_willPlaceOrder() {
        var holding = HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA");
        var expectedPrice = 19.74; // 20.0 / (1 + (stdDev * stdDevMultiplier / 100));
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));
        when(mockFutuAPIClient.getPendingOrders(anyLong())).thenReturn(List.of());

        stockTradingTask.executeTask();

        verify(mockFutuAPIClient, times(1)).placeOrder(anyLong(), eq(BUY), eq(stockCode), eq(4000), eq(expectedPrice));
        verify(mockTelegramAPIClient, times(1)).sendMessage(startsWith("Placed order (FundA): BUY"));
    }

    @Test
    void buyOrderPrice_mustLowerThanMarketPrice() {
        var holding = HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA");
        var marketPrice = 19;
        var maxBuyPrice = 18.88; // base / (stdDev * 1.5)
        var quote = new StockQuote(stockCode);

        quote.setPrice(String.valueOf(marketPrice));
        when(mockFutuAPIClient.getStockQuote(any())).thenReturn(quote);
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));
        when(mockFutuAPIClient.getPendingOrders(anyLong())).thenReturn(List.of());

        stockTradingTask.executeTask();

        verify(mockFutuAPIClient, times(1)).placeOrder(anyLong(), eq(BUY), eq(stockCode), eq(4000), eq(maxBuyPrice));
        verify(mockTelegramAPIClient, times(1)).sendMessage(startsWith("Placed order (FundA): BUY"));
    }


    @Test
    void pendingBuyOrderPriceOverThreshold_replaceOrder() {
        var holding = HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA");
        var expectedPrice = 19.74; // 20.0 / (1 + (stdDev * stdDevMultiplier / 100));
        var pendingOrderId = 123456L;
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));
        when(mockFutuAPIClient.getPendingOrders(anyLong())).thenReturn(List.of(
                Order.newOrder(stockCode, BUY, 4000, expectedPrice * 1.0021, pendingOrderId)
        ));

        stockTradingTask.executeTask();

        verify(mockFutuAPIClient, times(1)).cancelOrder(anyLong(), eq(pendingOrderId));
        verify(mockTelegramAPIClient, times(1)).sendMessage(startsWith("Cancelled order (FundA): BUY"));
        verify(mockFutuAPIClient, times(1)).placeOrder(anyLong(), eq(BUY), eq(stockCode), eq(4000), eq(expectedPrice));
        verify(mockTelegramAPIClient, times(1)).sendMessage(startsWith("Placed order (FundA): BUY"));
    }

    @Test
    void pendingBuyOrderPriceWithinThreshold_dontPlaceOrder() {
        var holding = HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA");
        var expectedPrice = 19.74; // 20.0 / (1 + (stdDev * stdDevMultiplier / 100));
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));
        when(mockFutuAPIClient.getPendingOrders(anyLong())).thenReturn(List.of(
                Order.newOrder(stockCode, BUY, 4000, expectedPrice * 1.0002, 123456L)   // price within threshold
        ));

        stockTradingTask.executeTask();

        verify(mockFutuAPIClient, never()).placeOrder(anyLong(), eq(BUY), any(), anyInt(), anyDouble());
        verify(mockTelegramAPIClient, never()).sendMessage(contains("placed order: BUY"));
    }

    @Test
    void multiPendingBuyOrders_cancelAllThenNew() {
        var holding = HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA");
        var expectedPrice = 19.74; // 20.0 / (1 + (stdDev * stdDevMultiplier / 100));
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));
        when(mockFutuAPIClient.getPendingOrders(anyLong())).thenReturn(List.of(
                Order.newOrder(stockCode, BUY, 4000, expectedPrice / 1.0002, 123456L),   // price within threshold,
                Order.newOrder(stockCode, BUY, 3500, expectedPrice / 1.1, 123456L)   // price within threshold
        ));

        stockTradingTask.executeTask();

        verify(mockFutuAPIClient, times(2)).cancelOrder(anyLong(), anyLong());
        verify(mockTelegramAPIClient, times(2)).sendMessage(startsWith("Cancelled order due to multiple pending (FundA): BUY"));
        verify(mockFutuAPIClient, times(1)).placeOrder(anyLong(), eq(BUY), eq(stockCode), eq(4000), eq(expectedPrice));
        verify(mockTelegramAPIClient, times(1)).sendMessage(startsWith("Placed order (FundA): BUY"));
    }


    @Test
    void cancelOrderFailed_stopProcessing() {
        var holding = HoldingStock.simple(stockCode, SELL, 4000, BigDecimal.valueOf(80000), "FundA");
        var expectedPrice = 19.72; // 20.0 / (1 + (stdDev * stdDevMultiplier / 100));
        var pendingOrderId = 123456L;
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));
        when(mockFutuAPIClient.getPendingOrders(anyLong())).thenReturn(List.of(
                Order.newOrder(stockCode, BUY, 4000, expectedPrice * 1.0021, pendingOrderId)
        ));
        when(mockFutuAPIClient.cancelOrder(anyLong(), anyLong()))
                .thenReturn(new FutuAPIClient.CancelOrderResponse(400, "error for testing"));

        stockTradingTask.executeTask();

        verify(mockFutuAPIClient, times(1)).cancelOrder(anyLong(), eq(pendingOrderId));
        verify(mockTelegramAPIClient, times(1)).sendMessage(contains("Cannot cancel order"));
        verify(mockFutuAPIClient, never()).placeOrder(anyLong(), any(), any(), anyInt(), anyDouble());
    }

    @Test
    void baseIsSellOrder_dontPlaceSellOrder() {
        var holding = HoldingStock.simple(stockCode, SELL, 4000, BigDecimal.valueOf(80000), "FundA");
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));

        stockTradingTask.executeTask();

        verify(mockFutuAPIClient, never()).placeOrder(anyLong(), eq(SELL), any(), anyInt(), anyDouble());
    }

    @Test
    void baseIsSell_priceLargerThanMinBuyPrice() {
        var holding = HoldingStock.simple(stockCode, SELL, 4000, BigDecimal.valueOf(80000), "FundA");
        var marketPrice = 22;
        var expectedPrice = 21.72; // nearest to (mkt price - 1 stdDev)
        var quote = new StockQuote(stockCode);

        quote.setPrice(String.valueOf(marketPrice));
        when(mockFutuAPIClient.getStockQuote(any())).thenReturn(quote);
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));

        stockTradingTask.executeTask();

        verify(mockFutuAPIClient, times(1)).placeOrder(anyLong(), eq(BUY), eq(stockCode), eq(4000), eq(expectedPrice));
    }

    @Test
    void baseIsSell_targetPriceLessThanPendingPrice_doNothing() {
        var holding = HoldingStock.simple(stockCode, SELL, 4000, BigDecimal.valueOf(80000), "FundA");
        var marketPrice = 22;
        var targetPrice = 21.72; // nearest to (mkt price - 1 stdDev)
        var quote = new StockQuote(stockCode);

        quote.setPrice(String.valueOf(marketPrice));
        when(mockFutuAPIClient.getStockQuote(any())).thenReturn(quote);
        when(mockFutuAPIClient.getPendingOrders(anyLong())).thenReturn(List.of(
                Order.newOrder(stockCode, BUY, 4000, targetPrice + 0.02, 123456L)
        ));
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));

        stockTradingTask.executeTask();

        verify(mockFutuAPIClient, never()).placeOrder(anyLong(), eq(BUY), any(), anyInt(), anyDouble());
    }

    @Test
    void noPendingSellOrder_placeOrder() {
        var holding = HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA");
        var expectedPrice = 20.26; // 20.0 * (1 + (stdDev * stdDevMultiplier / 100));
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));
        when(mockFutuAPIClient.getPendingOrders(anyLong())).thenReturn(List.of());

        stockTradingTask.executeTask();

        verify(mockFutuAPIClient, times(1)).placeOrder(anyLong(), eq(SELL), eq(stockCode), eq(4000), eq(expectedPrice));
    }

    @Test
    void pendingSellPriceWithinThreshold_doNothing() {
        var holding = HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA");
        var expectedPrice = 20.26; // 20.0 * (1 + (stdDev * stdDevMultiplier / 100));
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));
        when(mockFutuAPIClient.getPendingOrders(anyLong())).thenReturn(List.of(
                Order.newOrder(stockCode, SELL, 4000, expectedPrice / 1.0001, 123456L)
        ));

        stockTradingTask.executeTask();

        verify(mockFutuAPIClient, never()).placeOrder(anyLong(), eq(SELL), any(), anyInt(), anyDouble());
    }

    @Test
    void pendingSellPriceOverThreshold_replaceSellOrder() {
        var holding = HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA");
        var expectedPrice = 20.26; // 20.0 * (1 + (stdDev * stdDevMultiplier / 100));
        var pendingOrderId = 123456L;
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));
        when(mockFutuAPIClient.getPendingOrders(anyLong())).thenReturn(List.of(
                Order.newOrder(stockCode, BUY, 4000, expectedPrice * 1.0021, pendingOrderId)
        ));

        stockTradingTask.executeTask();

        verify(mockFutuAPIClient, times(1)).cancelOrder(anyLong(), eq(pendingOrderId));
        verify(mockTelegramAPIClient, times(1)).sendMessage(startsWith("Cancelled order (FundA): BUY"));
        verify(mockFutuAPIClient, times(1)).placeOrder(anyLong(), eq(SELL), eq(stockCode), eq(4000), eq(expectedPrice));
    }

    @Test
    void hasPartialFill_doNothing() {
        var holding = HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA");
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));
        when(mockFutuAPIClient.getPendingOrders(anyLong())).thenReturn(List.of(
                new Order(stockCode, BUY, 4000, 20.5, 123456L, 1000, 20.48, new Date()),
                Order.newOrder(stockCode, SELL, 4000, 19.5, 123456L)
        ));

        stockTradingTask.executeTask();

        verify(mockFutuAPIClient, never()).placeOrder(anyLong(), any(), any(), anyInt(), anyDouble());
        var logMatched = listAppender.list.stream().anyMatch(
                l -> l.getFormattedMessage().startsWith("Has partial filled pending order"));
        assertThat(logMatched).isTrue();
    }

    @Test
    void executeTask_willUnlockTrade() {
        var holding = HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA");
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));

        stockTradingTask.executeTask();

        verify(mockFutuAPIClient, atLeast(1)).unlockTrade(any());
    }

    @Test
    void unlockTradeFailed_willSendMessage() {
        var holding = HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA");
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));
        when(mockFutuAPIClient.unlockTrade(any())).thenReturn(false);

        stockTradingTask.executeTask();

        verify(mockTelegramAPIClient, times(1)).sendMessage(startsWith("StockTradingTask: Unexpected exception: unlock trade failed"));
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
        when(mockFutuAPIClient.getHKStockTodayExecutions(anyLong())).thenReturn(TdayExecutions);
        stockTradingTask.executeTask();
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

        stockTradingTask.executeTask();
        var logMatched2 = listAppender.list.stream().anyMatch(l -> l.getFormattedMessage().startsWith("base price: 19.5"));
        assertThat(logMatched2).isTrue();
    }

    @Test
    void partialFilledOrder_willSendWarningMessageToTelegram() {
        var holding = HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA");
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));
        var partialFilledOrder = new Order(stockCode, BUY, 4000, 20.5, 123456L, 3500, 20.48, new Date());
        when(mockFutuAPIClient.getPendingOrders(anyLong())).thenReturn(List.of(partialFilledOrder));

        stockTradingTask.executeTask();

        verify(mockTelegramAPIClient, times(1)).sendMessage(contains("WARN: Partial filled BUY 4000@20.50. filled=3500"));
    }
}
