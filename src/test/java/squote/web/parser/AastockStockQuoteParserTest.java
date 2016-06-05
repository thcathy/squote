package squote.web.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static squote.SquoteConstants.NA;

import com.google.common.base.Stopwatch;
import org.apache.commons.lang3.math.NumberUtils;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import squote.SquoteConstants;
import squote.domain.StockQuote;

public class AastockStockQuoteParserTest {
	private Logger log = LoggerFactory.getLogger(AastockStockQuoteParserTest.class);

	@Test
	public void getStockQuote_Given941_ShouldReturn941StockQuote() {
        Stopwatch timer = Stopwatch.createStarted();

		StockQuote q = new AastockStockQuoteParser("941").getStockQuote();
		assertEquals("941", q.getStockCode());
		assertEquals("CHINA MOBILE", q.getStockName());
		assertTrue(NumberUtils.isNumber(q.getPrice()));
		assertTrue(NumberUtils.isNumber(q.getChangeAmount().replace("+", "").replace("-", "")));
		assertTrue(q.getChange().endsWith("%"));
		assertTrue(NumberUtils.isNumber(q.getLow()));
		assertTrue(NumberUtils.isNumber(q.getHigh()));
		assertNotEquals(NA, q.getLastUpdate());
		assertTrue(NumberUtils.isNumber(q.getPe()));
		assertTrue(q.getYield().endsWith("%"));
		assertNotEquals(NA, q.getNAV());
		assertTrue(NumberUtils.isNumber(q.getYearLow()));
		assertTrue(NumberUtils.isNumber(q.getYearHigh()));

        log.debug("getStockQuote_Given941_ShouldReturn941StockQuote took: {}", timer.stop());
	}

    @Test
    public void getStockQuote_Given2800_ShouldReturn2800StockQuote() {
        Stopwatch timer = Stopwatch.createStarted();

        StockQuote q = new AastockStockQuoteParser("2800").getStockQuote();
        assertEquals("2800", q.getStockCode());
        assertEquals("TRACKER FUND", q.getStockName());
        assertTrue(NumberUtils.isNumber(q.getPrice()));
        assertTrue(NumberUtils.isNumber(q.getChangeAmount().replace("+", "").replace("-", "")));
        assertTrue(q.getChange().endsWith("%"));
        assertTrue(NumberUtils.isNumber(q.getLow()));
        assertTrue(NumberUtils.isNumber(q.getHigh()));
        assertNotEquals(NA, q.getLastUpdate());
        assertEquals(NA, q.getPe());
        assertTrue(q.getYield().endsWith("%"));
        assertEquals(NA, q.getNAV());
        assertTrue(NumberUtils.isNumber(q.getYearLow()));
        assertTrue(NumberUtils.isNumber(q.getYearHigh()));

        log.debug("getStockQuote_Given941_ShouldReturn2800StockQuote took: {}", timer.stop());
    }
}