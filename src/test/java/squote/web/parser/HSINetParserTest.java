package squote.web.parser;

import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.stream.IntStream;

import org.junit.Test;

import squote.SquoteConstants.IndexCode;
import thc.util.DateUtils;

public class HSINetParserTest {

	@Test
	public void parse_givenYesterday_ShouldReturnHSCEI() {
		Calendar calendar = Calendar.getInstance();
		Date weekday = IntStream.range(0, 10).mapToObj(i-> {
			calendar.add(Calendar.DATE, -1);
			return calendar;
		}).filter(x->!DateUtils.isWeekEnd(x)).findFirst().get().getTime();
		HSINetParser parser = new HSINetParser(IndexCode.HSCEI, weekday);
		
		assertTrue(parser.parse().get().getPriceDoubleValue() > 1);
	}
	
	@Test
	public void parse_givenYesterday_ShouldReturnHSI() {
		Calendar calendar = Calendar.getInstance();
		Date weekday = IntStream.range(0, 10).mapToObj(i-> {
			calendar.add(Calendar.DATE, -1);
			return calendar;
		}).filter(x->!DateUtils.isWeekEnd(x)).findFirst().get().getTime();
		HSINetParser parser = new HSINetParser(IndexCode.HSI, weekday);
		
		assertTrue(parser.parse().get().getPriceDoubleValue() > 1);
	}
}
