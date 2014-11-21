package squote.web.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import squote.SquoteConstants.IndexCode;
import squote.domain.StockQuote;

public class EtnetIndexParserTest {	
	@Test
	public void getGlobalIndexesReturnTwoQuotes() {
		List<StockQuote> indexes = new EtnetIndexQuoteParser().parse().get();
		assertEquals(IndexCode.HSI.name,indexes.get(0).getStockCode());
		assertTrue(indexes.get(0).getPriceDoubleValue() > 10000);
		
		assertEquals(IndexCode.HSCEI.name,indexes.get(1).getStockCode());
		assertTrue(indexes.get(1).getPriceDoubleValue() > 5000);
	}
}
