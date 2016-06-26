package squote.service;

import com.google.common.base.Stopwatch;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import squote.domain.StockQuote;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StockPerformanceServiceTest {
    private Logger log = LoggerFactory.getLogger(StockPerformanceServiceTest.class);

    private final ExecutorService executor = Executors.newFixedThreadPool(15);

    @Test
	public void getDetailStockQuoteWith3PreviousYearPrice_GivenStockCode_ShouldReturnWith3PreviouYearPrice() {
		Stopwatch timer = Stopwatch.createStarted();

		StockQuote quote = new StockPerformanceService(executor).getDetailStockQuoteWith3PreviousYearPrice("2828").get();
		assertEquals("2828", quote.getStockCode());
		assertTrue(quote.getPriceDoubleValue() > 0);
		assertTrue(quote.getLastYearPercentage() != 0);
		assertTrue(quote.getLast2YearPercentage() != 0);
		assertTrue(quote.getLast3YearPercentage() != 0);

		log.debug("getDetailStockQuoteWith3PreviousYearPrice_GivenStockCode_ShouldReturnWith3PreviouYearPrice took: {}", timer.stop());
	}
			
	@Test
	public void getStockPerformanceQuotes_ShouldReturnOne2828QuoteAndAllQuoteSortedByLastYearPercentageChg() {
        Stopwatch timer = Stopwatch.createStarted();

		StockPerformanceService service = new StockPerformanceService(executor);
		List<StockQuote> quotes = service.getStockPerformanceQuotes();
		assertTrue(quotes.size() > 50);
		
		List<StockQuote> quotes2828 = quotes.stream().filter(q -> "2828".equals(q.getStockCode())).collect(Collectors.toList());
		assertEquals("One 2828 quote should be found", 1, quotes2828.size());
				
		for (int i=0; i < quotes.size()-1; i++) {			
			assertTrue("Quotes are sort by last year percentage" , Double.compare(quotes.get(i).getLastYearPercentage(), quotes.get(i+1).getLastYearPercentage()) <=0);			
		}
				
		// Get the map again should given same obj
		assertEquals(quotes, service.getStockPerformanceQuotes());

        log.debug("getStockPerformanceQuotes_ShouldReturnOne2828QuoteAndAllQuoteSortedByLastYearPercentageChg took: {}", timer.stop());
	}
}
