package squote.web.parser;

import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Calendar;

import org.junit.Test;

public class HistoryQuoteParserTest {
	HistoryQuoteParser parser = new HistoryQuoteParser();

	@Test
    public void getPreviousYearQuote_GivenLastYear0001_ShouldReturnPriceOver50() {
		BigDecimal result = parser.getPreviousYearQuote("1", 1).get();		
		assertTrue(result.doubleValue() > 50);
	}
	
	@Test
	public void getQuoteAtDate_GivenHSIAndHSCEI_ShouldReturnCorrectPrice() {
		Calendar c = Calendar.getInstance();
		c.set(2013, 5, 10);	// 10 Jun 2013
		BigDecimal result = parser.getQuoteAtDate("%5EHSI", c, c).get();		
		assertTrue(result.doubleValue() == 21615.09);
		
		result = parser.getQuoteAtDate("%5EHSCE", c, c).get();
		assertTrue(result.doubleValue() == 10126.97);
	}
}
