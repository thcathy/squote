package squote.scheduletask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import squote.domain.DailyAssetSummary;
import squote.domain.DailyStockQuote;
import squote.domain.repository.DailyAssetSummaryRepository;
import squote.service.WebParserRestService;
import thc.util.MathUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ExecutionException;

@Component
public class CalculateDailySummaryTask {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Value(value = "${calculatedailysummarytask.enabled}") boolean enabled;
    @Value(value = "${calculatedailysummarytask.stdDevRange}") int stdDevRange;
    @Value(value = "${calculatedailysummarytask.codes}") String codes;
    static String CODE_SEPARATOR = ",";
    public static DateTimeFormatter rangeQuoteDateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    final DailyAssetSummaryRepository dailyAssetSummaryRepo;
    final WebParserRestService webService;

    public CalculateDailySummaryTask(DailyAssetSummaryRepository dailyAssetSummaryRepo, WebParserRestService webService) {
        this.dailyAssetSummaryRepo = dailyAssetSummaryRepo;
        this.webService = webService;
    }

    // @Scheduled(cron = "0 30 16 * * MON-FRI", zone = "Asia/Hong_Kong")
    public void executeTask() {
        if (!enabled) {
            log.info("Task disabled");
            return;
        }

        try {
            innerExecute();
        } catch (Exception e) {
            log.error("Unexpected exception!" ,e);
        }
    }

    public void innerExecute() throws ExecutionException, InterruptedException {
        log.info("Input: stdDevRange={}, codes={}", stdDevRange, codes);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String fromDate = LocalDate.now().minusDays(stdDevRange).format(formatter);
        String toDate = LocalDate.now().format(formatter);
        var today = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());

        for (String symbol : codes.split(CODE_SEPARATOR)) {
            log.info("Processing code: {}", symbol);
            var quotes = webService.getQuotesInRange(symbol, fromDate, toDate).get().getBody();
            var closingPrices = Arrays.stream(quotes).mapToDouble(DailyStockQuote::close).boxed().toList();
            var stdDev = MathUtils.calStdDev(closingPrices);
            log.info("stdDev of {} days: {}", stdDevRange, stdDev);

            var summary = new DailyAssetSummary(symbol, today);
            summary.stdDevs.put(stdDevRange, stdDev);
            dailyAssetSummaryRepo.save(summary);
            log.info("Saved: {} {} days stdDev={}", symbol, stdDevRange, stdDev);
        }
    }
}
