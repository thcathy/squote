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
	
	public static String QUOTES_KEY = "quotes";
	
	private static int expireAfterHour = 1;
	private volatile List<StockQuote> stockPerformanceQuotes;
	private Calendar expireOn = Calendar.getInstance();
	
	private final ExecutorService executor;
	
	public StockPerformanceService(ExecutorService executor) {
		super();
		this.executor = executor;
	}

	public synchronized List<StockQuote> getStockPerformanceQuotes() {
		// return result in cache before expire
		if (stockPerformanceQuotes != null && expireOn.getTime().compareTo(new Date()) > 0)
			return stockPerformanceQuotes;
		
		// get the result and store in cache		
		List<String> codes = Arrays.asList(IndexCode.values()).stream()
				.flatMap(l->l.constituents.stream())
				.distinct()
				.collect(Collectors.toList());
			
		codes.add("2828");
		stockPerformanceQuotes = codes.parallelStream()
				.map(c -> CompletableFuture.supplyAsync(() -> getDetailStockQuoteWith3PreviousYearPrice(c), executor))
				.map(f -> f.join())
				.sorted((x,y)->Double.compare(x.getLastYearPercentage(),y.getLastYearPercentage()))
				.collect(Collectors.toList());				
		
		expireOn = Calendar.getInstance();
		expireOn.add(Calendar.HOUR, expireAfterHour);
		return stockPerformanceQuotes;
	}
	
	public StockQuote getDetailStockQuoteWith3PreviousYearPrice(String code) {		
		StockQuote quote = new AastockStockQuoteParser(code).getStockQuote();		
		IntStream.rangeClosed(1, 3).forEach(i->
			new HistoryQuoteParser().getPreviousYearQuote(code, i).ifPresent(p->quote.setPreviousPrice(i, p.doubleValue()))
		);		
		return quote;
	}
		
}

