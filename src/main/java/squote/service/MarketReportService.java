package squote.service;

import static squote.SquoteConstants.IndexCode.HSCEI;
import static squote.SquoteConstants.IndexCode.HSI;
import static squote.web.parser.HSINetParser.Date;
import static squote.web.parser.HSINetParser.Index;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import squote.domain.MarketDailyReport;
import squote.domain.StockQuote;
import squote.domain.repository.MarketDailyReportRepository;
import squote.web.parser.HKMAMonetaryBaseParser;
import squote.web.parser.HSINetParser;
import thc.util.DateUtils;

public class MarketReportService {
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	static String DailyMonetaryBaseURL= "http://www.hkma.gov.hk/eng/market-data-and-statistics/monetary-statistics/monetary-base/{0,date,yyyy}/{0,date,yyyyMMdd}-2.shtml";
		
	private final MarketDailyReportRepository mktDailyRptRepo;
	private final CentralWebQueryService queryService;
	
	public MarketReportService(MarketDailyReportRepository repo, CentralWebQueryService quoteService) {
		this.queryService = quoteService;
		this.mktDailyRptRepo = repo;
	}
	
	public static Calendar pre(int amount, int field) { 
		Calendar c = Calendar.getInstance();
		c.add(field, 0-amount);
		return c;
	}
				
	public MarketDailyReport getMarketDailyReportOnBefore(Calendar calendar) {		
		Optional<MarketDailyReport> reportOption = IntStream.range(0, 10).mapToObj(i-> {
					calendar.add(Calendar.DATE, -1);
					if (DateUtils.isWeekEnd(calendar)) 
						return MarketDailyReport.EMPTY_REPORT;
					else 
						return getMarketDailyReportFromDb(calendar).orElse(getMarketDailyReportFromWebAndPersist(calendar));
				}).filter(x->!x.isEmpty()).findFirst();
		
		return reportOption.orElse(new MarketDailyReport(calendar.getTime()));		
	}

	private Optional<MarketDailyReport> getMarketDailyReportFromDb(Calendar calendar) {						 		
		return Optional.ofNullable(mktDailyRptRepo.findByDate(MarketDailyReport.formatDate(calendar.getTime())));		
	}
	
	private MarketDailyReport getMarketDailyReportFromWebAndPersist(Calendar calendar) {		
		Optional<StockQuote> hsi = new HSINetParser(Index(HSI), Date(calendar.getTime())).parse();
		Optional<MarketDailyReport> r = new HSINetParser(Index(HSI), Date(calendar.getTime())).parse()
				.map(i -> HKMAMonetaryBaseParser.retrieveMonetaryBase(calendar.getTime()))
				.map(m -> new MarketDailyReport(calendar.getTime(), 
						m.get(), 
						hsi.get(), new HSINetParser(Index(HSCEI), Date(calendar.getTime())).parse().get())
				);								
		r.ifPresent(x -> mktDailyRptRepo.save(x));
		return r.orElse(MarketDailyReport.EMPTY_REPORT);
	}

	public MarketDailyReport getTodayMarketDailyReport() { return getMarketDailyReportOnBefore(Calendar.getInstance()); }
	
	public List<CompletableFuture<MarketDailyReport>> getMarketDailyReport(Calendar... from) {
		return Arrays.stream(from).map(c-> 
					queryService.submit( ()->getMarketDailyReportOnBefore(c) )
				)
				.collect(Collectors.toList());		
	}
}
    
