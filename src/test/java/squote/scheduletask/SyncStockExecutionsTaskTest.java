package squote.scheduletask;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import squote.SquoteConstants;
import squote.domain.HoldingStock;
import squote.domain.repository.HoldingStockRepository;
import squote.service.EmailService;
import squote.service.FutuAPIClient;

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
    FutuAPIClient mockFutuAPIClient = Mockito.mock(FutuAPIClient.class);

    @BeforeEach
    void init() {
        when(mockFactory.build(any(), anyShort())).thenReturn(mockFutuAPIClient);

        task = new SyncStockExecutionsTask();
        task.enabled = true;
        task.futuAPIClientFactory = mockFactory;
        task.holdingRepo = mockHoldingStockRepository;
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
        assertTrue(date.getTime() < new Date().getTime() - (30L * 24 * 60 * 60 * 1000));
    }

    @Test
    void executeTask_usePreviousHoldingDate()
    {
        var holding = new HoldingStock("", "", SquoteConstants.Side.BUY, 1, null, new Date(), null);
        when(mockHoldingStockRepository.findTopByFundNameOrderByDateDesc(any()))
                .thenReturn(Optional.of(holding));
        task.executeTask();
        ArgumentCaptor<Date> argumentCaptor = ArgumentCaptor.forClass(Date.class);
        verify(mockFutuAPIClient).getHKStockExecutions(anyLong(), argumentCaptor.capture());
        var date = argumentCaptor.getValue();
        assertEquals(holding.getDate(), date);
    }
}
