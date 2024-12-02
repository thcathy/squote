package squote.scheduletask;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import squote.SquoteConstants;
import squote.domain.HoldingStock;
import squote.domain.TaskConfig;
import squote.domain.repository.FundRepository;
import squote.domain.repository.HoldingStockRepository;
import squote.domain.repository.TaskConfigRepository;
import squote.service.EmailService;
import squote.service.FutuAPIClient;
import squote.service.UpdateFundByHoldingService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyShort;
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
        when(mockFactory.build(any(), anyShort())).thenReturn(mockFutuAPIClient);
        when(mockHoldingStockRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mockFundRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        task = new SyncStockExecutionsTask();
        task.enabled = true;
        task.futuAPIClientFactory = mockFactory;
        task.holdingRepo = mockHoldingStockRepository;
        task.taskConfigRepo = mockTaskConfigRepository;
        task.updateFundService = Mockito.mock(UpdateFundByHoldingService.class);
        task.fundRepo = mockFundRepository;
        task.emailService = emailService;
        task.clientConfigJson = """
        [
            {"ip":"127.0.0.1","port":1,"fundName":"A"}
        ]""";
    }

    @Test
    void executeTask_canBeDisabled() {
        task.enabled = false;
        task.executeTask();
        verify(mockFactory, never()).build(any(), anyShort());
    }

    @Test
    void executeTask_missingClientConfig() {
        task.clientConfigJson = "";
        task.executeTask();
        verify(mockFactory, never()).build(any(), anyShort());
    }

    @Test
    void executeTask_noPreviousDate_query1MonthBefore()
    {
        when(mockHoldingStockRepository.findTopByFundNameOrderByDateDesc(any()))
                .thenReturn(Optional.empty());
        task.executeTask();
        ArgumentCaptor<Date> argumentCaptor = ArgumentCaptor.forClass(Date.class);
        verify(mockFutuAPIClient).getHKStockExecutions(anyLong(), argumentCaptor.capture());
        var date = argumentCaptor.getValue();
        assertTrue(date.getTime() < new Date().getTime() - (27L * 24 * 60 * 60 * 1000));    // 1 month minus 1 day earlier 
    }

    @Test
    void executeTask_useTimeFromConfig() throws ParseException {
        var holding = new HoldingStock("", "", SquoteConstants.Side.BUY, 1, null, new Date(), null);
        var formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        var config = new SyncStockExecutionsTask.SyncStockExecutionsTaskConfig(formatter.parse("2024-11-01 00:00:00"));
        var taskConfig = new TaskConfig(SyncStockExecutionsTask.class.toString(), config.toJson(config));
        when(mockHoldingStockRepository.findTopByFundNameOrderByDateDesc(any()))
                .thenReturn(Optional.of(holding));
        when(mockTaskConfigRepository.findById(any())).thenReturn(Optional.of(taskConfig));
        task.executeTask();

        ArgumentCaptor<Date> argumentCaptor = ArgumentCaptor.forClass(Date.class);
        verify(mockFutuAPIClient).getHKStockExecutions(anyLong(), argumentCaptor.capture());
        var date = argumentCaptor.getValue();
        assertEquals(formatter.parse("2024-11-02 00:00:00"), date);
    }
}
