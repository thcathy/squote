package squote.scheduletask;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import squote.SquoteConstants;
import squote.domain.Execution;
import squote.domain.HoldingStock;
import squote.domain.Market;
import squote.domain.TaskConfig;
import squote.domain.repository.FundRepository;
import squote.domain.repository.HoldingStockRepository;
import squote.domain.repository.TaskConfigRepository;
import squote.service.EmailService;
import squote.service.FutuAPIClient;
import squote.service.UpdateFundByHoldingService;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SyncStockExecutionsTaskTest {
    SyncStockExecutionsTask task;
    FutuAPIClientFactory mockFactory = Mockito.mock(FutuAPIClientFactory.class);
    EmailService emailService = Mockito.mock(EmailService.class);
    HoldingStockRepository mockHoldingStockRepository = Mockito.mock(HoldingStockRepository.class);
    TaskConfigRepository mockTaskConfigRepository = Mockito.mock(TaskConfigRepository.class);
    FundRepository mockFundRepository = Mockito.mock(FundRepository.class);
    FutuAPIClient mockFutuAPIClient = Mockito.mock(FutuAPIClient.class);

    @BeforeEach
    void init() {
        when(mockFactory.build(any())).thenReturn(mockFutuAPIClient);
        when(mockHoldingStockRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mockFundRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        task = new SyncStockExecutionsTask();
        task.enabledByMarket = Map.of("HK", true);
        task.futuAPIClientFactory = mockFactory;
        task.holdingRepo = mockHoldingStockRepository;
        task.taskConfigRepo = mockTaskConfigRepository;
        task.updateFundService = Mockito.mock(UpdateFundByHoldingService.class);
        task.fundRepo = mockFundRepository;
        task.emailService = emailService;
        task.clientConfigJson = """
        [
            {"ip":"127.0.0.1","port":1,"fundUserId":"userA","fundName":"A","accountId":1,"unlockCode":"","markets":["HK"]}
        ]""";
    }

    @Test
    void executeTask_canBeDisabled() {
        task.enabledByMarket = Map.of("HK", false);
        task.executeHK();
        verify(mockFactory, never()).build(any());
    }

    @Test
    void executeTask_missingClientConfig() {
        task.clientConfigJson = "";
        task.executeHK();
        verify(mockFactory, never()).build(any());
    }

    @Test
    void executeTask_noPreviousDate_query1MonthBefore()
    {
        when(mockHoldingStockRepository.findTopByFundNameOrderByDateDesc(any()))
                .thenReturn(Optional.empty());
        task.executeHK();
        ArgumentCaptor<Date> dateCaptor = ArgumentCaptor.forClass(Date.class);
        ArgumentCaptor<Market> marketCaptor = ArgumentCaptor.forClass(Market.class);
        verify(mockFutuAPIClient).getRecentExecutions(dateCaptor.capture(), marketCaptor.capture());
        var date = dateCaptor.getValue();
        var market = marketCaptor.getValue();
        assertTrue(date.getTime() < new Date().getTime() - (27L * 24 * 60 * 60 * 1000));    // 1 month minus 1 day earlier
        assertEquals(Market.HK, market);
    }

    @Test
    void executeTask_useTimeFromConfig() throws ParseException {
        var holding = new HoldingStock("", "", SquoteConstants.Side.BUY, 1, null, new Date(), null);
        var formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        var lastExecutionTimes = Map.of(Market.HK, formatter.parse("2024-11-01 00:00:00.000"));
        var config = new SyncStockExecutionsTaskConfig(lastExecutionTimes);
        var taskConfig = new TaskConfig(SyncStockExecutionsTask.class.toString(), SyncStockExecutionsTaskConfig.toJson(config));
        when(mockHoldingStockRepository.findTopByFundNameOrderByDateDesc(any()))
                .thenReturn(Optional.of(holding));
        when(mockTaskConfigRepository.findById(any())).thenReturn(Optional.of(taskConfig));
        task.executeHK();

        ArgumentCaptor<Date> dateCaptor = ArgumentCaptor.forClass(Date.class);
        ArgumentCaptor<Market> marketCaptor = ArgumentCaptor.forClass(Market.class);
        verify(mockFutuAPIClient).getRecentExecutions(dateCaptor.capture(), marketCaptor.capture());
        var date = dateCaptor.getValue();
        var market = marketCaptor.getValue();
        assertEquals(formatter.parse("2024-11-01 00:00:00.000"), date);
        assertEquals(Market.HK, market);
    }

    @Test
    void executeTask_sendSummaryToEmail() {
        task.summaryEmailAddress = "tester@test.com";
        task.executeHK();
        verify(emailService, times(1)).sendEmail(any(), any(), any());
    }

    @Test
    void executeTask_wontSendEmailIfNoSetup() {
        task.summaryEmailAddress = "";
        task.executeHK();
        verify(emailService, never()).sendEmail(any(), any(), any());
    }

    @Test
    void executeUS_canBeDisabled() {
        task.enabledByMarket = Map.of("US", false);
        task.executeUS();
        verify(mockFactory, never()).build(any());
    }

    @Test
    void executeUS_usesUSMarket() {
        task.enabledByMarket = Map.of("US", true);
        task.clientConfigJson = """
        [
            {"ip":"127.0.0.1","port":1,"fundUserId":"userA","fundName":"A","accountId":1,"unlockCode":"","markets":["US"]}
        ]""";
        task.executeUS();
        ArgumentCaptor<Date> dateCaptor = ArgumentCaptor.forClass(Date.class);
        ArgumentCaptor<Market> marketCaptor = ArgumentCaptor.forClass(Market.class);
        verify(mockFutuAPIClient).getRecentExecutions(dateCaptor.capture(), marketCaptor.capture());
        var market = marketCaptor.getValue();
        assertEquals(Market.US, market);
    }

    @Test
    void executeTask_executionsAreSortedByTime() {
        var userId = UUID.randomUUID().toString();
        task.userId = userId;
        var mockUpdateFundService = Mockito.mock(UpdateFundByHoldingService.class);
        task.updateFundService = mockUpdateFundService;
        when(mockUpdateFundService.updateFundByHolding(any(), any(), any(), any())).thenReturn(new squote.domain.Fund(userId, "A"));

        var executions = new HashMap<String, Execution>();

        // Create 3 executions with out-of-order timestamps
        var baseTime = System.currentTimeMillis();
        var exec3 = new Execution();
        exec3.setOrderId("order3");
        exec3.setFillIds("fill3");
        exec3.setQuantity(BigDecimal.valueOf(100));
        exec3.setPrice(BigDecimal.valueOf(25));
        exec3.setSide(squote.SquoteConstants.Side.BUY);
        exec3.setCode("2800");
        exec3.setTime(baseTime + 2000); // Newest
        exec3.setMarket(Market.HK);
        executions.put(exec3.getOrderId(), exec3);

        var exec1 = new Execution();
        exec1.setOrderId("order1");
        exec1.setFillIds("fill1");
        exec1.setQuantity(BigDecimal.valueOf(100));
        exec1.setPrice(BigDecimal.valueOf(23));
        exec1.setSide(squote.SquoteConstants.Side.BUY);
        exec1.setCode("2800");
        exec1.setTime(baseTime); // Oldest
        exec1.setMarket(Market.HK);
        executions.put(exec1.getOrderId(), exec1);

        var exec2 = new Execution();
        exec2.setOrderId("order2");
        exec2.setFillIds("fill2");
        exec2.setQuantity(BigDecimal.valueOf(100));
        exec2.setPrice(BigDecimal.valueOf(24));
        exec2.setSide(squote.SquoteConstants.Side.BUY);
        exec2.setCode("2800");
        exec2.setTime(baseTime + 1000); // Middle
        exec2.setMarket(Market.HK);
        executions.put(exec2.getOrderId(), exec2);

        when(mockFutuAPIClient.getRecentExecutions(any(Date.class), eq(Market.HK))).thenReturn(executions);

        task.executeHK();

        // Verify holdings are saved in chronological order
        ArgumentCaptor<HoldingStock> holdingCaptor = ArgumentCaptor.forClass(HoldingStock.class);
        verify(mockHoldingStockRepository, times(3)).save(holdingCaptor.capture());

        var savedHoldings = holdingCaptor.getAllValues();
        assertEquals(3, savedHoldings.size());
        assertEquals("fill1", savedHoldings.get(0).getFillIds());
        assertEquals("fill2", savedHoldings.get(1).getFillIds());
        assertEquals("fill3", savedHoldings.get(2).getFillIds());
    }
}
