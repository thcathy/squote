package squote.web.parser;

import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.Test;

import squote.SquoteConstants.IndexCode;
import squote.domain.StockQuote;
import thc.util.DateUtils;

public class HSINetParserTest {

	@Test
	public void parse_givenYesterday_ShouldReturnHSCEI() {
		Optional<Boolean> hsceiFound = IntStream.range(0, 10).mapToObj(i-> {
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DATE, -i-1);
			return c;
		})
				.filter(c->!DateUtils.isWeekEnd(c))
				.map(c -> c.getTime())
				.map(t -> new HSINetParser(IndexCode.HSCEI, t))
				.map(p -> {
					Optional<StockQuote> quote = p.parse();
					return quote.isPresent() && quote.get().getPriceDoubleValue() > 1;
				})
				.filter(x -> x)
				.findFirst();
		assertTrue(hsceiFound.get());
	}
	
	@Test
	public void parse_givenYesterday_ShouldReturnHSI() {
		Optional<Boolean> hsiFound = IntStream.range(0, 10).mapToObj(i-> {
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DATE, -i-1);
			return c;
		})
				.filter(c->!DateUtils.isWeekEnd(c))
				.map(c -> c.getTime())
				.map(t -> new HSINetParser(IndexCode.HSI, t))
				.map(p -> {
					Optional<StockQuote> quote = p.parse();
					return quote.isPresent() && quote.get().getPriceDoubleValue() > 1;
				})
				.filter(x -> x)
				.findFirst();
		assertTrue(hsiFound.get());
	}
}