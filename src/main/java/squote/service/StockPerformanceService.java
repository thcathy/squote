package squote.service;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import squote.SquoteConstants.IndexCode;
import squote.domain.StockQuote;
import squote.web.parser.AastockStockQuoteParser;
import squote.web.parser.HistoryQuoteParser;

public class StockPerformanceService {
	public static String HCEI_KEY = "hcei";
	public static String QUOTES_KEY = "quotes";
	
	private static int expireAfterHour = 1;
	private volatile Map<String, Object> stockPerformanceMap;
	private Calendar expireOn = Calendar.getInstance();
	
	private final ExecutorService executor;
	
	public StockPerformanceService(ExecutorService executor) {
		super();
		this.executor = executor;
	}

	public synchronized Map<String, Object> getStockPerformanceMap() {
		// return result in cache before expire
		if (stockPerformanceMap != null && expireOn.getTime().compareTo(new Date()) > 0)
			return stockPerformanceMap;
		
		CompletableFuture<StockQuote> hceiETFFuture = CompletableFuture.supplyAsync(() -> getDetailStockQuoteWith3PreviousYearPrice("2828"), executor);
				
		// get the result and store in cache		
		stockPerformanceMap = new HashMap<String, Object>();		
		stockPerformanceMap.put(QUOTES_KEY, getIndexContituents());
		stockPerformanceMap.put(HCEI_KEY, hceiETFFuture.join());
		expireOn = Calendar.getInstance();
		expireOn.add(Calendar.HOUR, expireAfterHour);
		return stockPerformanceMap;
	}
		
	public List<StockQuote> getIndexContituents() {
		List<String> codes = Arrays.asList(IndexCode.values()).stream()
			.flatMap(l->l.constituents.stream())
			.distinct()
			.collect(Collectors.toList());
		
		return codes.parallelStream()
			.map(c -> CompletableFuture.supplyAsync(() -> getDetailStockQuoteWith3PreviousYearPrice(c), executor))
			.map(f -> f.join())
			.sorted((x,y)->(int)(x.getLastYearPercentage()-y.getLastYearPercentage()))
			.collect(Collectors.toList());				
	}

	public StockQuote getDetailStockQuoteWith3PreviousYearPrice(String code) {		
		StockQuote quote = new AastockStockQuoteParser(code).getStockQuote();		
		IntStream.rangeClosed(1, 3).forEach(i->
			new HistoryQuoteParser().getPreviousYearQuote(code, i).ifPresent(p->quote.setPreviousPrice(i, p.doubleValue()))
		);		
		return quote;
	}
		
}

