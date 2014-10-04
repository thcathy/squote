package squote.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import squote.domain.StockQuote;
import squote.web.parser.AastockStockQuoteParser;
import squote.web.parser.HistoryQuoteParser;
import squote.web.parser.IndexConstituentParser;
import thc.util.BeanUtils;

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
		Set<String> stockCodes = new HashSet<String>();
		IndexConstituentParser codesParser = new IndexConstituentParser();
		stockCodes.addAll(codesParser.getHSIConstituents());
		stockCodes.addAll(codesParser.getHCCIConstituents());
		stockCodes.addAll(codesParser.getHCEIConstituents());
		stockCodes.addAll(codesParser.getMSCIChinaConstituents());
				
		List<GetDetailStockQuoteRunner> runners = new ArrayList<GetDetailStockQuoteRunner>();
		for (String code : stockCodes) runners.add(new GetDetailStockQuoteRunner(code));
		List<StockQuote> quotes = queryService.executeCallables(runners);
		
		Collections.sort(quotes, BeanUtils.getCompare("getLastYearPercentage"));		
		return quotes;
	}

	public StockQuote getDetailStockQuoteWith3PreviousYearPrice(String code) {		
		StockQuote quote = new AastockStockQuoteParser(code).getStockQuote();
		HistoryQuoteParser historyParser = new HistoryQuoteParser();
		for (int i = 1; i <= 3; i++) {
			BigDecimal price = historyParser.getPreviousYearQuote(code, i);
			if (price != null) quote.setPreviousPrice(i, price.doubleValue());
		}
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

