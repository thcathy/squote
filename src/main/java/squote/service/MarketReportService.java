package squote.service;

import static squote.web.parser.HSINetParser.Date;
import static squote.web.parser.HSINetParser.Index;
import static squote.web.parser.HSINetParser.IndexCode.HSCEI;
import static squote.web.parser.HSINetParser.IndexCode.HSI;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import squote.domain.MarketDailyReport;
import squote.domain.MonetaryBase;
import squote.domain.StockQuote;
import squote.domain.repository.MarketDailyReportRepository;
import squote.web.parser.HKMAMonetaryBaseParser;
import squote.web.parser.HSINetParser;
import thc.util.DateUtils;

import com.google.common.base.Optional;

public class MarketReportService {
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	static String DailyMonetaryBaseURL= "http://www.hkma.gov.hk/eng/market-data-and-statistics/monetary-statistics/monetary-base/{0,date,yyyy}/{0,date,yyyyMMdd}-2.shtml";
		
	private final MarketDailyReportRepository mktDailyRptRepo;
	private final CentralWebQueryService quoteService;
	
	public MarketReportService(MarketDailyReportRepository repo, CentralWebQueryService quoteService) {
		this.quoteService = quoteService;
		this.mktDailyRptRepo = repo;
	}
	
	public static Calendar pre(int amount, int field) { 
		Calendar c = Calendar.getInstance();
		c.add(field, 0-amount);
		return c;
	}
				
	public MarketDailyReport getPreviousMarketDailyReport(Calendar calendar) {
		calendar.add(Calendar.DATE, 1); 
		for (int i=0; i < 10; i++)		
		{
			calendar.add(Calendar.DATE, -1); 
			if (DateUtils.isWeekEnd(calendar)) continue;
				
			// if db contain, return			
			MarketDailyReport dbReport = mktDailyRptRepo.findByDate(MarketDailyReport.formatDate(calendar.getTime()));
			if (dbReport != null) return dbReport;
						
			Optional<StockQuote> hsi = new HSINetParser(Index(HSI), Date(calendar.getTime())).parse();
			if (!hsi.isPresent()) continue;
			
			Optional<MonetaryBase> monetaryBase = HKMAMonetaryBaseParser.retrieveMonetaryBase(calendar.getTime());
			if (monetaryBase.isPresent()) {
				MarketDailyReport report = new MarketDailyReport(calendar.getTime(), 
						monetaryBase.get(), 
						hsi.get(), new HSINetParser(Index(HSCEI), Date(calendar.getTime())).parse().get());
				mktDailyRptRepo.save(report);
				return report;
			}			 
		}		
		return new MarketDailyReport(calendar.getTime());
	}
	
	public MarketDailyReport getTodayMarketDailyReport() { return getPreviousMarketDailyReport(Calendar.getInstance()); }
	
	public List<Future<MarketDailyReport>> getMarketDailyReport(Calendar... from) {
		List<Future<MarketDailyReport>> futures = new ArrayList<Future<MarketDailyReport>>();
		for (final Calendar c : from) {
			futures.add(quoteService.submit(new Callable<MarketDailyReport>() {
				public MarketDailyReport call() throws Exception {
					return getPreviousMarketDailyReport(c);
				}				
			}));
		}
		return futures;
	}
}
    
