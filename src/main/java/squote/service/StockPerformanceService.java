package squote.service;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import squote.domain.StockQuote;
import squote.web.parser.AastockStockQuoteParser;
import squote.web.parser.HistoryQuoteParser;
import squote.web.parser.IndexConstituentParser;

public class StockPerformanceService {
	private static int expireAfterHour = 1;
	private volatile Map<String, Object> stockPerformanceMap;
	private Calendar expireOn = Calendar.getInstance();
	
	private final CentralWebQueryService queryService;
	
	public StockPerformanceService(CentralWebQueryService queryService) {
		super();
		this.queryService = queryService;
	}

	public synchronized Map<String, Object> getStockPerformanceMap() {
		// return result in cache before expire
		if (stockPerformanceMap != null && expireOn.getTime().compareTo(new Date()) > 0)
			return stockPerformanceMap;
		
		// get the result and store in cache
		stockPerformanceMap = new HashMap<String, Object>();
		stockPerformanceMap.put("hcei", getHceiETF());
		stockPerformanceMap.put("quotes", getIndexContituents());				
		expireOn = Calendar.getInstance();
		expireOn.add(Calendar.HOUR, expireAfterHour);
		return stockPerformanceMap;
	}
	
	public StockQuote getHceiETF() {		
		return getDetailStockQuoteWith3PreviousYearPrice("2828");
	}

	public List<StockQuote> getIndexContituents() {
		IndexConstituentParser codesParser = new IndexConstituentParser();
		List<GetDetailStockQuoteRunner> runners = Arrays.asList(
				codesParser.getHSIConstituents()
				,codesParser.getHCCIConstituents()
				,codesParser.getHCEIConstituents()
				,codesParser.getMSCIChinaConstituents())
				.stream()
				.flatMap(x->x.stream())
				.map(x->new GetDetailStockQuoteRunner(x)).collect(Collectors.toList());
				
		List<StockQuote> quotes = queryService.executeCallables(runners);
		
		Collections.sort(quotes, (x,y)->(int)(x.getLastYearPercentage()-y.getLastYearPercentage()));		
		return quotes;
	}

	public StockQuote getDetailStockQuoteWith3PreviousYearPrice(String code) {		
		StockQuote quote = new AastockStockQuoteParser(code).getStockQuote();		
		IntStream.rangeClosed(1, 3).forEach(i->
			new HistoryQuoteParser().getPreviousYearQuote(code, i).ifPresent(p->quote.setPreviousPrice(i, p.doubleValue()))
		);		
		return quote;
	}
		
	// TODO: change to use central query service or any others
	class GetDetailStockQuoteRunner implements Callable<StockQuote> {
		String code;
		GetDetailStockQuoteRunner(String code) {this.code = code;}
		@Override public StockQuote call() {
			return getDetailStockQuoteWith3PreviousYearPrice(code);
		}
	}	
}

