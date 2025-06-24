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
import squote.domain.repository.FundRepository;
import squote.service.FutuAPIClient;
import squote.service.StockTradingAlgoService;
import squote.service.TelegramAPIClient;

import java.util.Arrays;

import static org.mockito.Mockito.*;

class StockTradingTaskTest {
    FundRepository mockFundRepo = Mockito.mock(FundRepository.class);
    FutuAPIClientFactory mockFactory = Mockito.mock(FutuAPIClientFactory.class);
    FutuAPIClient mockFutuAPIClient = Mockito.mock(FutuAPIClient.class);
    TelegramAPIClient mockTelegramAPIClient = Mockito.mock(TelegramAPIClient.class);
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

        stockTradingTask = new StockTradingTask(mockFundRepo, mockStockTradingAlgoService, mockTelegramAPIClient);
        stockTradingTask.futuAPIClientFactory = mockFactory;
        stockTradingTask.enabled = true;  // Enable the task for testing
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

    @Test
    void testExecuteTaskDisabled() {
        stockTradingTask.enabled = false;
        stockTradingTask.executeTask();

        verifyNoInteractions(mockStockTradingAlgoService);
        verifyNoInteractions(mockFundRepo);
    }

    @Test
    void executeTask_willCloseFutuConnection() {
        stockTradingTask.executeTask();

        verify(mockFutuAPIClient, atLeast(1)).close();
    }

    @Test
    void executeTask_willUnlockTrade() {
        stockTradingTask.executeTask();

        verify(mockFutuAPIClient, atLeast(1)).unlockTrade(any());
    }

    @Test
    void unlockTradeFailed_willSendMessage() {
        when(mockFutuAPIClient.unlockTrade(any())).thenReturn(false);

        stockTradingTask.executeTask();

        verify(mockTelegramAPIClient, times(1)).sendMessage(startsWith("StockTradingTask: Unexpected exception: unlock trade failed"));
    }

    @Test
    void executeTask_willCallProcessSingleSymbol() {
        stockTradingTask.executeTask();

        verify(mockStockTradingAlgoService, atLeast(1))
                .processSingleSymbol(any(), any(), any(), any());
    }
}
