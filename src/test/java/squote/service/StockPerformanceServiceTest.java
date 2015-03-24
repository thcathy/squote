package squote.service;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;

import squote.domain.StockQuote;

public class StockPerformanceServiceTest {
	
	@Test
	public void getDetailStockQuoteWith3PreviousYearPrice_GivenStockCode_ShouldReturnWith3PreviouYearPrice() {
		StockQuote quote = new StockPerformanceService(Executors.newFixedThreadPool(50)).getDetailStockQuoteWith3PreviousYearPrice("2828");
		assertEquals("2828", quote.getStockCode());
		assertTrue(quote.getPriceDoubleValue() > 0);
		assertTrue(quote.getLastYearPercentage() != 0);
		assertTrue(quote.getLast2YearPercentage() != 0);
		assertTrue(quote.getLast3YearPercentage() != 0);		
	}
			
	@Test
	public void getStockPerformanceQuotes_ShouldReturnOne2828QuoteAndAllQuoteSortedByLastYearPercentageChg() {
		StockPerformanceService service = new StockPerformanceService(Executors.newFixedThreadPool(50));		
		List<StockQuote> quotes = service.getStockPerformanceQuotes();
		assertTrue(quotes.size() > 50);
		
		List<StockQuote> quotes2828 = quotes.stream().filter(q -> "2828".equals(q.getStockCode())).collect(Collectors.toList());
		assertEquals("One 2828 quote should be found", 1, quotes2828.size());
				
		for (int i=0; i < quotes.size()-1; i++) {			
			assertTrue("Quotes are sort by last year percentage" , Double.compare(quotes.get(i).getLastYearPercentage(), quotes.get(i+1).getLastYearPercentage()) <=0);			
		}
				
		// Get the map again should given same obj
		assertEquals(quotes, service.getStockPerformanceQuotes());
	}
}
