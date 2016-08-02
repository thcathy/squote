package squote.web.parser;

import org.junit.Test;
import squote.SquoteConstants.IndexCode;
import squote.domain.StockQuote;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EtnetIndexParserTest {	
	@Test
	public void parse_ShouldReturnHSIAndHSCEI() {
		List<StockQuote> indexes = new EtnetIndexQuoteParser().parse().get();
		assertEquals(IndexCode.HSI.name,indexes.get(0).getStockCode());
		assertTrue(noChange(indexes.get(0).getPrice()) || indexes.get(0).getPriceDoubleValue() > 10000);
		
		assertEquals(IndexCode.HSCEI.name,indexes.get(1).getStockCode());
		assertTrue(noChange(indexes.get(1).getPrice()) || indexes.get(1).getPriceDoubleValue() > 5000);
	}

	private boolean noChange(String value) {
		return "--".equals(value);
	}
}
