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
import squote.domain.DailyAssetSummary;
import squote.domain.HoldingStock;
import squote.domain.Order;
import squote.domain.repository.DailyAssetSummaryRepository;
import squote.domain.repository.FundRepository;
import squote.domain.repository.HoldingStockRepository;
import squote.service.FutuAPIClient;
import squote.service.TelegramAPIClient;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static squote.SquoteConstants.Side.BUY;
import static squote.SquoteConstants.Side.SELL;

class StockTradingTaskTest {
    DailyAssetSummaryRepository dailyAssetSummaryRepo = Mockito.mock(DailyAssetSummaryRepository.class);
    FundRepository fundRepo = Mockito.mock(FundRepository.class);
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

        StockTradingTaskProperties properties = new StockTradingTaskProperties();
        properties.fundSymbols = Map.of("FundA", List.of(stockCode));

        stockTradingTask = new StockTradingTask(dailyAssetSummaryRepo, fundRepo, holdingStockRepository, mockTelegramAPIClient, properties);
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
    }

    static Stream<TestFindBasePriceData> testFindBasePriceDataProvider() {
        return Stream.of(
                // single buy, pick that one
                new TestFindBasePriceData(List.of(
                        HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA")
                ), "base price: 20.0"),
                // multi buy, pick the lowest price
                new TestFindBasePriceData(List.of(
                        HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA"),
                        HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(88000), "FundA")
                ), "base price: 20.0"),
                // no buy, 1 sell, pick sell price
                new TestFindBasePriceData(List.of(
                        HoldingStock.simple(stockCode, SELL, 4000, BigDecimal.valueOf(80000), "FundA")
                ), "base price: 20.0"),
                // 1 buy, 1 sell, pick sell price
                new TestFindBasePriceData(List.of(
                        HoldingStock.simple(stockCode, SELL, 4000, BigDecimal.valueOf(88000), "FundA"),
                        HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA")
                ), "base price: 22.0"),
                // n buy, 1 sell, pick n-1 buy price
                new TestFindBasePriceData(List.of(
                        HoldingStock.simple(stockCode, SELL, 4000, BigDecimal.valueOf(88000), "FundA"),
                        HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA"),
                        HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(88000), "FundA")
                ), "base price: 22.0"),
                // n buy, m sell, pick n-m buy price
                new TestFindBasePriceData(List.of(
                        HoldingStock.simple(stockCode, SELL, 4000, BigDecimal.valueOf(88000), "FundA"),
                        HoldingStock.simple(stockCode, SELL, 4000, BigDecimal.valueOf(98000), "FundA"),
                        HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA"),
                        HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(90000), "FundA"),
                        HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(100000), "FundA")
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
                        HoldingStock.simple(stockCode, SELL, 4000, BigDecimal.valueOf(80000), "FundA"),
                        HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(88000), "FundA")
                ));
        stockTradingTask.executeTask();
        var logMatched = listAppender.list.stream().anyMatch(
                l -> l.getFormattedMessage().startsWith("Unexpected executions: SELL code1 4000@20.00 (isToday=false) < BUY code1 4000@22.00 (isToday=false)"));
        assertThat(logMatched).isTrue();
    }

    @Test
    void testExecuteTaskDisabled() {
        stockTradingTask.enabled = false;
        stockTradingTask.executeTask();

        verifyNoInteractions(dailyAssetSummaryRepo);
        verifyNoInteractions(fundRepo);
        verifyNoInteractions(holdingStockRepository);
    }

    @Test
    void testExecutionToStringMethod() {
        StockTradingTask.Execution execution = new StockTradingTask.Execution("GOOG", SELL, 50, 123.45, false);
        String expectedString = "SELL GOOG 50@123.45 (isToday=false)";

        assertThat(execution.toString()).isEqualTo(expectedString);
    }

    @Test
    void executeTask_willCloseFutuConnection() {
        var holding = HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA");
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));

        stockTradingTask.executeTask();

        verify(mockFutuAPIClient, times(1)).close();
    }

    @Test
    void noPendingBuyOrder_willPlaceOrder() {
        var holding = HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA");
        var expectedPrice = 19.72; // 20.0 / (1 + (stdDev * stdDevMultiplier / 100));
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));
        when(mockFutuAPIClient.getPendingOrders(anyLong())).thenReturn(List.of());

        stockTradingTask.executeTask();

        verify(mockFutuAPIClient, times(1)).placeOrder(anyLong(), eq(BUY), eq(stockCode), eq(4000), eq(expectedPrice));
        verify(mockTelegramAPIClient, times(1)).sendMessage(contains("placed order: BUY"));
    }

    @Test
    void pendingBuyOrderPriceOverThreshold_replaceOrder() {
        var holding = HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA");
        var expectedPrice = 19.72; // 20.0 / (1 + (stdDev * stdDevMultiplier / 100));
        var pendingOrderId = 123456L;
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));
        when(mockFutuAPIClient.getPendingOrders(anyLong())).thenReturn(List.of(
                Order.newOrder(stockCode, BUY, 4000, expectedPrice * 1.0021, pendingOrderId)
        ));

        stockTradingTask.executeTask();

        verify(mockFutuAPIClient, times(1)).cancelOrder(anyLong(), eq(pendingOrderId));
        verify(mockTelegramAPIClient, times(1)).sendMessage(contains(" cancelled"));
        verify(mockFutuAPIClient, times(1)).placeOrder(anyLong(), eq(BUY), eq(stockCode), eq(4000), eq(expectedPrice));
        verify(mockTelegramAPIClient, times(1)).sendMessage(contains("placed order: BUY"));
    }

    @Test
    void pendingBuyOrderPriceWithinThreshold_dontPlaceOrder() {
        var holding = HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA");
        var expectedPrice = 19.72; // 20.0 / (1 + (stdDev * stdDevMultiplier / 100));
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));
        when(mockFutuAPIClient.getPendingOrders(anyLong())).thenReturn(List.of(
                Order.newOrder(stockCode, BUY, 4000, expectedPrice * 1.0002, 123456L)   // price within threshold
        ));

        stockTradingTask.executeTask();

        verify(mockFutuAPIClient, never()).placeOrder(anyLong(), eq(BUY), any(), anyInt(), anyDouble());
        verify(mockTelegramAPIClient, never()).sendMessage(contains("placed order: BUY"));
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
    void noPendingSellOrder_placeOrder() {
        var holding = HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA");
        var expectedPrice = 20.28; // 20.0 * (1 + (stdDev * stdDevMultiplier / 100));
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));
        when(mockFutuAPIClient.getPendingOrders(anyLong())).thenReturn(List.of());

        stockTradingTask.executeTask();

        verify(mockFutuAPIClient, times(1)).placeOrder(anyLong(), eq(SELL), eq(stockCode), eq(4000), eq(expectedPrice));
    }

    @Test
    void pendingSellPriceWithinThreshold_doNothing() {
        var holding = HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA");
        var expectedPrice = 20.28; // 20.0 * (1 + (stdDev * stdDevMultiplier / 100));
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
        var expectedPrice = 20.28; // 20.0 * (1 + (stdDev * stdDevMultiplier / 100));
        var pendingOrderId = 123456L;
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));
        when(mockFutuAPIClient.getPendingOrders(anyLong())).thenReturn(List.of(
                Order.newOrder(stockCode, BUY, 4000, expectedPrice * 1.0021, pendingOrderId)
        ));

        stockTradingTask.executeTask();

        verify(mockFutuAPIClient, times(1)).cancelOrder(anyLong(), eq(pendingOrderId));
        verify(mockTelegramAPIClient, times(1)).sendMessage(contains("cancelled"));
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

        verify(mockFutuAPIClient, times(1)).unlockTrade(any());
    }

    @Test
    void unlockTradeFailed_willSendMessage() {
        var holding = HoldingStock.simple(stockCode, BUY, 4000, BigDecimal.valueOf(80000), "FundA");
        when(holdingStockRepository.findByUserIdOrderByDate("UserA")).thenReturn(List.of(holding));
        when(mockFutuAPIClient.unlockTrade(any())).thenReturn(false);

        stockTradingTask.executeTask();

        verify(mockTelegramAPIClient, times(1)).sendMessage(startsWith("StockTradingTask: Unexpected exception: unlock trade failed"));
    }
}
