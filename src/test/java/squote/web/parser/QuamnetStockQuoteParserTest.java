package squote.web.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.math.NumberUtils;
import org.junit.Test;

import squote.domain.StockQuote;

public class QuamnetStockQuoteParserTest {	
	@Test	
	public void getStockQuote_Given941_ShouldReturn941StockQuote() {
		StockQuote q = new QuamnetStockQuoteParser("941").getStockQuote();
		assertEquals("941", q.getStockCode());
		assertEquals("CHINA MOBILE", q.getStockName());
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
	}
}