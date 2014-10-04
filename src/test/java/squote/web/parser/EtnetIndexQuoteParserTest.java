package squote.web.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import squote.domain.StockQuote;

public class EtnetIndexQuoteParserTest {	
	@Test
	public void getGlobalIndexesReturnTwoQuotes() {
		List<StockQuote> indexes = new EtnetIndexQuoteParser().getGlobalIndexes();
		assertEquals("Hang Seng Index",indexes.get(0).getStockCode());
		assertTrue(indexes.get(0).getPriceDoubleValue() > 10000);
		
		assertEquals("HS China Enterprises Index",indexes.get(1).getStockCode());
		assertTrue(indexes.get(1).getPriceDoubleValue() > 5000);
	}
}
