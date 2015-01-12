package squote.web.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static squote.web.parser.HSINetParser.Date;
import static squote.web.parser.HSINetParser.Index;

import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;

import squote.SquoteConstants;
import squote.domain.MonetaryBase;
import squote.domain.StockQuote;

public class MarketDailyReportParsersTest {	
	
	@Test
    public void retrieveNormalDateMonetaryBase() throws Exception {
		MonetaryBase result = HKMAMonetaryBaseParser.retrieveMonetaryBase(DateUtils.parseDate("20141230","yyyyMMdd")).get();		
		assertEquals(341105, result.getIndebtedness(),0);
		assertEquals(11346, result.getNotes(),0);
		assertEquals(239243, result.getClosingBalance(),0);
		assertEquals(753289, result.getExchangeFund(),0);
		assertEquals(1344983, result.getTotal(),0);
	}
	
	@Test
	public void retrieveDataFromHolidayShouldGivenAbsent() throws Exception {
		Date holiday = DateUtils.parseDate("20131215","yyyyMMdd");
		assertFalse(HKMAMonetaryBaseParser.retrieveMonetaryBase(holiday).isPresent());
		assertFalse(new HSINetParser(Index(SquoteConstants.IndexCode.HSCEI), Date(holiday)).parse().isPresent());
	}
	
	@Test
	public void retrieveIndexOnWeekDate() throws Exception {
		testRetrieveIndex(SquoteConstants.IndexCode.HSCEI);
		testRetrieveIndex(SquoteConstants.IndexCode.HSI);
	}

	private void testRetrieveIndex(SquoteConstants.IndexCode index) {
		int retry = 0;
		Calendar date = Calendar.getInstance();
		Optional<StockQuote> quote;
		do 
		{
			retry++;
			date.add(Calendar.DATE, -1);
			quote = new HSINetParser(Index(index), Date(date.getTime())).parse();
		} while (!quote.isPresent() && retry < 7);
		assertTrue(NumberUtils.isNumber(quote.get().getLastUpdate()));
		assertTrue(NumberUtils.isNumber(quote.get().getHigh()));
		assertTrue(NumberUtils.isNumber(quote.get().getLow()));
		assertTrue(NumberUtils.isNumber(quote.get().getPrice()));
		assertTrue(NumberUtils.isNumber(quote.get().getChangeAmount()));
		assertTrue(NumberUtils.isNumber(quote.get().getChange()));
		assertTrue(NumberUtils.isNumber(quote.get().getYield()));
		assertTrue(NumberUtils.isNumber(quote.get().getPe()));
	}
		
}