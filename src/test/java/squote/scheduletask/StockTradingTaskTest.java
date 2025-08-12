package squote.scheduletask;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import squote.domain.AlgoConfig;
import squote.domain.Fund;
import squote.domain.StockQuote;
import squote.domain.repository.FundRepository;
import squote.service.FutuAPIClient;
import squote.service.StockTradingAlgoService;
import squote.service.TelegramAPIClient;
import squote.service.TiingoAPIClient;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;

class StockTradingTaskTest {
    FundRepository mockFundRepo = Mockito.mock(FundRepository.class);
    FutuAPIClientFactory mockFactory = Mockito.mock(FutuAPIClientFactory.class);
    FutuAPIClient mockFutuAPIClient = Mockito.mock(FutuAPIClient.class);
    TelegramAPIClient mockTelegramAPIClient = Mockito.mock(TelegramAPIClient.class);
    TiingoAPIClient mockTiingoAPIClient = Mockito.mock(TiingoAPIClient.class);
    StockTradingAlgoService mockStockTradingAlgoService = Mockito.mock(StockTradingAlgoService.class);

    private StockTradingTask stockTradingTask;
    private ListAppender<ILoggingEvent> listAppender;
    private static final String stockCode = "code1";

    @BeforeEach
    void setUp() {
        listAppender = new ListAppender<>();
        listAppender.setName("testAppender");
        listAppender.start();

        Logger logger = (Logger) LoggerFactory.getLogger(StockTradingTask.class);
        logger.addAppender(listAppender);

        when(mockFactory.build(any())).thenReturn(mockFutuAPIClient);
        when(mockFutuAPIClient.unlockTrade(any())).thenReturn(true);
        when(mockTiingoAPIClient.getPrices(any())).thenReturn(CompletableFuture.completedFuture(List.of()));

        stockTradingTask = new StockTradingTask(mockFundRepo, mockStockTradingAlgoService, mockTelegramAPIClient, mockTiingoAPIClient);
        stockTradingTask.futuAPIClientFactory = mockFactory;
        stockTradingTask.enabledByMarket = new HashMap<>();
        stockTradingTask.enabledByMarket.put("HK", true);
        stockTradingTask.enabledByMarket.put("US", true);
        stockTradingTask.clientConfigJson = """
                    [
                        {"fundName": "FundA", "fundUserId": "UserA", "accountId": 1, "ip": "192.0.0.1", "port": 80, "unlockCode": "dummy code"},
                        {"fundName": "FundB", "fundUserId": "UserB", "accountId": 2, "ip": "192.0.0.1", "port": 80, "unlockCode": "dummy code"}
                    ]
                """;

        var algoConfig = new AlgoConfig(stockCode, 3500, null, 10, 0.7, null);
        var fundA = new Fund("dummy", "FundA");
        fundA.putAlgoConfig(stockCode, algoConfig);
        var fundB = new Fund("dummy", "FundB");
        fundB.putAlgoConfig(stockCode, algoConfig);
        when(mockFundRepo.findAll()).thenReturn(Arrays.asList(fundA, fundB));
    }

    @Test
    void testExecuteHKDisabled() {
        stockTradingTask.enabledByMarket.put("HK", false);
        stockTradingTask.executeHK();

        verifyNoInteractions(mockStockTradingAlgoService);
        verifyNoInteractions(mockFundRepo);
    }

    @Test
    void executeHK_willCloseFutuConnection() {
        stockTradingTask.executeHK();

        verify(mockFutuAPIClient, atLeast(1)).close();
    }

    @Test
    void executeHK_willUnlockTrade() {
        stockTradingTask.executeHK();

        verify(mockFutuAPIClient, atLeast(1)).unlockTrade(any());
    }

    @Test
    void unlockTradeFailed_willSendMessage() {
        when(mockFutuAPIClient.unlockTrade(any())).thenReturn(false);

        stockTradingTask.executeHK();

        verify(mockTelegramAPIClient, times(1)).sendMessage(startsWith("StockTradingTask - HK: Unexpected exception: unlock trade failed"));
    }

    @Test
    void executeHK_willCallProcessSingleSymbol() {
        stockTradingTask.executeHK();

        verify(mockStockTradingAlgoService, atLeast(1))
                .processSingleSymbol(any(), any(), any(), any(), any(), any());
    }

    @Test
    void executeUS_withUSStocks_willCallTiingoAPI() {
        var algoConfig = new AlgoConfig("QQQ.US", 3500, null, 10, 0.7, null);
        var fundA = new Fund("dummy", "FundA");
        fundA.putAlgoConfig("QQQ.US", algoConfig);
        when(mockFundRepo.findAll()).thenReturn(List.of(fundA));
        
        var mockQuote = new StockQuote("QQQ");
        mockQuote.setPrice("150.00");
        when(mockTiingoAPIClient.getPrices(List.of("QQQ")))
                .thenReturn(CompletableFuture.completedFuture(List.of(mockQuote)));

        stockTradingTask.executeUS();

        verify(mockTiingoAPIClient, times(1)).getPrices(List.of("QQQ"));
        verify(mockStockTradingAlgoService, atLeast(1))
                .processSingleSymbol(any(), any(), any(), any(), any(), eq(mockQuote));
    }

    @Test
    void executeHK_withNoUSStocks_willNotCallTiingoAPI() {
        stockTradingTask.executeHK();

        verify(mockTiingoAPIClient, never()).getPrices(any());
        verify(mockStockTradingAlgoService, atLeast(1))
                .processSingleSymbol(any(), any(), any(), any(), any(), isNull());
    }
}
