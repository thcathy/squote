package squote.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import squote.domain.MarketDailyReport;
import squote.domain.MonetaryBase;
import squote.domain.StockQuote;
import squote.domain.repository.MarketDailyReportRepository;
import thc.util.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class MarketReportService {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final ExecutorService pool = Executors.newFixedThreadPool(5);
	private final MarketDailyReportRepository mktDailyRptRepo;
	private final WebParserRestService webService;

	public MarketReportService(MarketDailyReportRepository repo, WebParserRestService webService) {
		this.webService = webService;
		this.mktDailyRptRepo = repo;
	}
	
	public static Calendar pre(int amount, int field) { 
		Calendar c = Calendar.getInstance();
		c.add(field, 0-amount);
		return c;
	}
				
	public MarketDailyReport getMarketDailyReportOnBefore(Calendar calendar) {
		Optional<MarketDailyReport> reportOption =
					tenPreviousDayStreamFrom(calendar)
						.filter(DateUtils::notWeekEnd)
						.map(this::convertToyyyymmdd)
						.map(c -> getMarketDailyReportFromDb(c).orElse(getMarketDailyReportFromWebAndPersist(c)))
						.filter(x->!x.isEmpty()).findFirst();
		
		return reportOption.orElse(new MarketDailyReport(calendar.getTime()));		
	}

	private Stream<Calendar> tenPreviousDayStreamFrom(Calendar calendar) {
		return IntStream.range(0, 10).mapToObj(i-> {
			Calendar c = (Calendar) calendar.clone();
			c.add(Calendar.DATE, -i-1);
			return c;
		});
	}

	private String convertToyyyymmdd(Calendar c) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		return dateFormat.format(c.getTime());
	}

	private Optional<MarketDailyReport> getMarketDailyReportFromDb(String yyyymmdd) {
		return Optional.ofNullable(mktDailyRptRepo.findByDate(Integer.valueOf(yyyymmdd)));
	}
	
	private MarketDailyReport getMarketDailyReportFromWebAndPersist(String yyyymmdd) {
		MarketDailyReport report = MarketDailyReport.EMPTY_REPORT;
		try {
			MonetaryBase monetaryBase = webService.getHKMAReport(yyyymmdd).get().getBody();

			if (monetaryBase.getTotal() > 0.1) {
				StockQuote[] hsiReports = webService.getHSINetReports(yyyymmdd).get().getBody();
				report = new MarketDailyReport(yyyymmdd, monetaryBase, hsiReports[0], hsiReports[1]);
			}
		} catch (Exception e) {
			log.debug("Exception when getting market daily report from web", e);
		}

		if (!report.isEmpty()) mktDailyRptRepo.save(report);
		return report;
	}

	public MarketDailyReport getTodayMarketDailyReport() { return getMarketDailyReportOnBefore(Calendar.getInstance()); }
	
	public List<CompletableFuture<MarketDailyReport>> getMarketDailyReport(Calendar... from) {
		return Arrays.stream(from)
				.map(c -> CompletableFuture.supplyAsync( ()->getMarketDailyReportOnBefore(c), pool ))
				.collect(Collectors.toList());
	}
}