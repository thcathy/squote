package squote.scheduletask;

import com.mashape.unirest.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import squote.domain.DailyStockQuote;
import squote.domain.repository.DailyAssetSummaryRepository;
import squote.service.WebParserRestService;

import java.time.LocalDate;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalculateDailySummaryTaskTest {

    @InjectMocks
    private CalculateDailySummaryTask calculateDailySummaryTask;

    @Mock private WebParserRestService webService;
    @Mock private DailyAssetSummaryRepository dailyAssetSummaryRepository;

    private boolean enabled = true;
    private int stdDevRange = 30;
    private String codes = "2800,2828";

    @BeforeEach
    void setUp() {
        calculateDailySummaryTask.enabled = enabled;
        calculateDailySummaryTask.stdDevRange = stdDevRange;
        calculateDailySummaryTask.codes = codes;
    }

    public static DailyStockQuote quoteWithClose(double close) {
        return new DailyStockQuote(new Date(), 0.0, close, 0.0, 0.0, 0, 0.0);
    }

    @Test
    void testExecuteTaskEnabled() {
        DailyStockQuote[] mockQuotes = {
            quoteWithClose(100.0), quoteWithClose(110.0), quoteWithClose(120.0), quoteWithClose(130.0), quoteWithClose(140.0)
        };
        HttpResponse<DailyStockQuote[]> mockResponse = mock(HttpResponse.class);
        when(mockResponse.getBody()).thenReturn(mockQuotes);
        when(webService.getQuotesInRange(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        calculateDailySummaryTask.executeTask();
        var expectedFromDate = LocalDate.now().minusDays(stdDevRange).format(CalculateDailySummaryTask.rangeQuoteDateFormatter);
        var expectedToDate = LocalDate.now().format(CalculateDailySummaryTask.rangeQuoteDateFormatter);

        verify(webService, times(2)).getQuotesInRange(any(), eq(expectedFromDate), eq(expectedToDate));
        verify(dailyAssetSummaryRepository, times(2)).save(any());
    }

    @Test
    void testExecuteTaskDisabled() {
        calculateDailySummaryTask.enabled = false;
        calculateDailySummaryTask.executeTask();
        verify(webService, never()).getQuotesInRange(any(), any(), any());
    }

}
