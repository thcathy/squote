package squote.service;

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StockPerformanceServiceTest {
    private Logger log = LoggerFactory.getLogger(StockPerformanceServiceTest.class);

	// TODO: change to check constituents call
    //@Test
	public void getDetailStockQuoteWith3PreviousYearPrice_GivenStockCode_ShouldReturnWith3PreviouYearPrice() {
		Stopwatch timer = Stopwatch.createStarted();

		/*StockQuote quote = new StockPerformanceService(null).getDetailStockQuoteWith3PreviousYearPrice("2828").get();
		assertEquals("2828", quote.getStockCode());
		assertTrue(quote.getPriceDoubleValue() > 0);
		assertTrue(quote.getLastYearPercentage() != 0);
		assertTrue(quote.getLast2YearPercentage() != 0);
		assertTrue(quote.getLast3YearPercentage() != 0);*/

		log.debug("getDetailStockQuoteWith3PreviousYearPrice_GivenStockCode_ShouldReturnWith3PreviouYearPrice took: {}", timer.stop());
	}

}
