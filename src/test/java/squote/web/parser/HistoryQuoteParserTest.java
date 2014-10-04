package squote.web.parser;

import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Calendar;

import org.junit.Test;

public class HistoryQuoteParserTest {
	HistoryQuoteParser parser = new HistoryQuoteParser();

	@Test
    public void testGetPreviousYearQuote() {
		BigDecimal result = parser.getPreviousYearQuote("1", 1);		
		assertTrue(result.doubleValue() > 50);
	}
	
	@Test
	public void testGetPreviousIndexOnExactDate() {
		Calendar c = Calendar.getInstance();
		c.set(2013, 5, 10);	// 10 Jun 2013
		BigDecimal result = parser.getQuoteAtDate("%5EHSI", c, c);		
		assertTrue(result.doubleValue() == 21615.09);
		
		result = parser.getQuoteAtDate("%5EHSCE", c, c);
		assertTrue(result.doubleValue() == 10126.97);
	}
}
