package squote.web.parser;

import static org.junit.Assert.*;

import com.google.common.base.Stopwatch;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import squote.controller.repository.HoldingStockControllerTest;
import squote.domain.StockQuote;

public class EtnetStockQuoteParserTest {
	private Logger log = LoggerFactory.getLogger(EtnetStockQuoteParserTest.class);

	@Test
	public void getStockQuote_Given941_ShouldReturn941StockQuote() {
		Stopwatch timer = Stopwatch.createStarted();

		StockQuote q = new EtnetStockQuoteParser().parse("941").get();
		assertEquals("941", q.getStockCode());
		assertTrue(NumberUtils.isNumber(q.getPrice()));
		assertTrue(NumberUtils.isNumber(q.getChangeAmount().replace("+", "").replace("-", "")));		
		assertTrue(q.getChange().endsWith("%"));
		assertTrue(NumberUtils.isNumber(q.getLow()));
		assertTrue(NumberUtils.isNumber(q.getHigh()));
		assertNotEquals("NA", q.getLastUpdate());
		assertTrue(NumberUtils.isNumber(q.getPe()));
		assertTrue(q.getYield().endsWith("%"));
		assertNotEquals("NA", q.getNAV());
		assertTrue(NumberUtils.isNumber(q.getYearLow()));
		assertTrue(NumberUtils.isNumber(q.getYearHigh()));

		log.debug("getStockQuote_Given941_ShouldReturn941StockQuote took: " + timer.stop());
	}
}