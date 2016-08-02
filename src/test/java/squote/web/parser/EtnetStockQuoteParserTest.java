package squote.web.parser;

import com.google.common.base.Stopwatch;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.util.StringUtils;
import squote.domain.StockQuote;

import static org.junit.Assert.*;

public class EtnetStockQuoteParserTest {
	private Logger log = LoggerFactory.getLogger(EtnetStockQuoteParserTest.class);

	//@Test
	public void getStockQuote_Given941_ShouldReturn941StockQuote() {
		Stopwatch timer = Stopwatch.createStarted();

		StockQuote q = new EtnetStockQuoteParser().parse("941").get();
		assertEquals("941", q.getStockCode());
		assertTrue(NumberUtils.isNumber(q.getPrice()));
		assertTrue(noChange(q.getChangeAmount()) || NumberUtils.isNumber(q.getChangeAmount().replace("+", "").replace("-", "")));
		assertTrue(noChange(q.getChange()) || q.getChange().endsWith("%"));
		assertTrue(StringUtils.isEmptyOrWhitespace(q.getLow()) || NumberUtils.isNumber(q.getLow()));
		assertTrue(StringUtils.isEmptyOrWhitespace(q.getHigh()) || NumberUtils.isNumber(q.getHigh()));
		assertNotEquals("NA", q.getLastUpdate());
		assertTrue(NumberUtils.isNumber(q.getPe()));
		assertTrue(q.getYield().endsWith("%"));
		assertNotEquals("NA", q.getNAV());
		assertTrue(NumberUtils.isNumber(q.getYearLow()));
		assertTrue(NumberUtils.isNumber(q.getYearHigh()));

		log.debug("getStockQuote_Given941_ShouldReturn941StockQuote took: " + timer.stop());
	}

	private boolean noChange(String value) {
		return "--".equals(value);
	}
}