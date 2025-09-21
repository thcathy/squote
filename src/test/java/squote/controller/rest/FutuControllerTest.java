package squote.controller.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import squote.domain.FlowSummaryInfo;
import squote.domain.Market;
import squote.scheduletask.FutuClientConfig;
import squote.security.AuthenticationService;
import squote.service.FutuAPIClient;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FutuControllerTest {
    @Mock private AuthenticationService authenticationService;
    @Mock private FutuAPIClient futuAPIClient;

    @Mock private FutuController.FutuAPIClientFactory futuAPIClientFactory;

    @InjectMocks private FutuController futuController;

    private final long accountId = 12345L;

    @BeforeEach
    void setUp() {
        futuController.rsaKey = "test-rsa-key";
        futuController.clientConfigJson = """
                [
                    {
                        "ip": "127.0.0.1",
                        "port": 11111,
                        "fundUserId": "test-user",
                        "fundName": "test-fund",
                        "accountId": 12345,
                        "unlockCode": "123456",
                        "markets": ["HK", "US"]
                    }
                ]
                """;
        futuController.getFlowSummaryDelayMs = 1;
        futuController.futuAPIClientFactory = futuAPIClientFactory;
        futuController.init();

        when(authenticationService.getUserId()).thenReturn(Optional.of("test-user-123"));
        lenient().when(futuAPIClientFactory.build(any(FutuClientConfig.class))).thenReturn(futuAPIClient);
    }

    @Test
    void getFlowSummary_SingleDay_Success() throws ParseException {
        var expectedFlowSummary = createMockFlowSummaryList();
        when(futuAPIClient.getFlowSummary(any(Date.class), eq(Market.HK))).thenReturn(expectedFlowSummary);

        var result = futuController.getFlowSummary(accountId, "HK", "20240115", "20240115");
        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(expectedFlowSummary);
    }

    @Test
    void getFlowSummary_MultipleDays_Success() throws ParseException {
        var dailyFlowSummary = createMockFlowSummaryList();
        when(futuAPIClient.getFlowSummary(any(Date.class), eq(Market.US))).thenReturn(dailyFlowSummary);

        var result = futuController.getFlowSummary(accountId, "US", "20240115", "20240117"); // 3 days
        assertThat(result).hasSize(6); // 2 entries per day
        verify(futuAPIClient, times(3)).getFlowSummary(any(Date.class), eq(Market.US));
        verify(futuAPIClient, times(1)).close();
    }

    @Test
    void getFlowSummary_EnsuresClientIsClosed() throws ParseException {
        when(futuAPIClient.getFlowSummary(any(Date.class), eq(Market.HK))).thenThrow(new RuntimeException("API Error"));
        futuController.getFlowSummary(accountId, "HK", "20240115", "20240115");
        verify(futuAPIClient, times(1)).close();
    }

    @Test
    void getFlowSummary_RespectsRateLimitDelay() throws ParseException {
        futuController.getFlowSummaryDelayMs = 50;
        var dailyFlowSummary = createMockFlowSummaryList();
        when(futuAPIClient.getFlowSummary(any(Date.class), eq(Market.HK))).thenReturn(dailyFlowSummary);

        var startTime = System.currentTimeMillis();
        futuController.getFlowSummary(accountId, "HK", "20240115", "20240117");
        var endTime = System.currentTimeMillis();

        assertThat(endTime - startTime).isGreaterThanOrEqualTo(2L * futuController.getFlowSummaryDelayMs);
    }

    private List<FlowSummaryInfo> createMockFlowSummaryList() {
        return Arrays.asList(
                new FlowSummaryInfo(
                        "20240115",
                        "20240117",
                        "HKD",
                        "DIVIDEND",
                        "IN",
                        new BigDecimal("1000.00"),
                        "Dividend payment",
                        "FLOW001"
                ),
                new FlowSummaryInfo(
                        "20240115",
                        "20240117",
                        "HKD",
                        "FEE",
                        "OUT",
                        new BigDecimal("50.00"),
                        "Trading fee",
                        "FLOW002"
                )
        );
    }
}
