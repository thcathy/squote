package squote.service;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import org.junit.Test;

import squote.domain.StockQuote;

public class StockPerformanceServiceTest {
		
	@Test
	public void verifyGetStockPerformanceMap() {
		StockPerformanceService service = new StockPerformanceService(Executors.newFixedThreadPool(50));
		Map<String, Object> resultMap = service.getStockPerformanceMap();
		
		StockQuote hcei = (StockQuote) resultMap.get(StockPerformanceService.HCEI_KEY);
		assertEquals("2828", hcei.getStockCode());
		assertTrue(hcei.getPriceDoubleValue() > 0);
		assertTrue(hcei.getLastYearPercentage() > 0);
		assertTrue(hcei.getLast2YearPercentage() > 0);
		assertTrue(hcei.getLast3YearPercentage() > 0);
		
		@SuppressWarnings("unchecked")
		List<StockQuote> quotes = (List<StockQuote>) resultMap.get(StockPerformanceService.QUOTES_KEY);
		assertTrue(quotes.size() > 50);
		
		
		// Get the map again should given same obj
		assertEquals(resultMap, service.getStockPerformanceMap());
	}
}
