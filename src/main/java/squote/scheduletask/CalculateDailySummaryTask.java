package squote.scheduletask;

import com.mashape.unirest.http.exceptions.UnirestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
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
import java.util.List;
import java.util.concurrent.ExecutionException;

@Component
public class CalculateDailySummaryTask {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Value(value = "${calculatedailysummarytask.enabled}") boolean enabled;
    @Value(value = "${calculatedailysummarytask.stdDevRange}") List<Integer> stdDevRanges;
    @Value(value = "${calculatedailysummarytask.codes}") List<String> codes;

    public static DateTimeFormatter rangeQuoteDateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    final DailyAssetSummaryRepository dailyAssetSummaryRepo;
    final WebParserRestService webService;

    public CalculateDailySummaryTask(DailyAssetSummaryRepository dailyAssetSummaryRepo, WebParserRestService webService) {
        this.dailyAssetSummaryRepo = dailyAssetSummaryRepo;
        this.webService = webService;
    }

    @Scheduled(cron = "0 20 18 * * MON-FRI", zone = "Asia/Hong_Kong")
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

    public void innerExecute() throws ExecutionException, InterruptedException, UnirestException {
        log.info("Input: stdDevRange={}, codes={}", stdDevRanges, codes);
        var maxStdDevRange = stdDevRanges.stream().mapToInt(s -> s).max().orElse(20);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String fromDate = LocalDate.now().minusDays(maxStdDevRange).format(formatter);
        String toDate = LocalDate.now().plusDays(1).format(formatter);
        var today = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());

        for (String symbol : codes) {
            log.info("Processing code: {}, {}-{}", symbol, fromDate, toDate);
            var quotes = webService.getQuotesInRange(symbol, fromDate, toDate).getBody();
            var closingPrices = Arrays.stream(quotes).mapToDouble(DailyStockQuote::close).boxed().toList();
            var summary = new DailyAssetSummary(symbol, today);

            for (int stdDevRange : stdDevRanges) {
                var closingPricesSubList = closingPrices.stream()
                        .skip(Math.max(0, closingPrices.size() - stdDevRange))
                        .toList();
                var stdDev = MathUtils.calStdDev(closingPricesSubList);
                log.info("stdDev of {} days: {}", stdDevRange, stdDev);
                summary.stdDevs.put(stdDevRange, stdDev);
            }

            dailyAssetSummaryRepo.save(summary);
            log.info("Saved: {} for {}", symbol, today);
        }
    }
}
