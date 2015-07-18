package squote.domain;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class FundTest {
	public static String FUND_NAME = "Winner";
		
	private Fund createSimpleFund() {
		Fund f = new Fund(FUND_NAME);
		f.buyStock("2828", 500, new BigDecimal(50000));
		f.buyStock("2800", 1000, new BigDecimal(25000));
		return f;
	}
	
	@Test
	public void calculateNetProfit_givenQuoteMap_ShouldRunCorrectly() {
		StockQuote hcei = new StockQuote("2828");
		hcei.setPrice("120");
		StockQuote hsi = new StockQuote("2800");
		hsi.setPrice("26");
		Map<String, StockQuote> quoteMap = new HashMap<>();
		quoteMap.put(hcei.getStockCode(), hcei);
		quoteMap.put(hsi.getStockCode(), hsi);
		Fund f = createSimpleFund();
		
		Fund fundWithNetProfit = f.calculateNetProfit(quoteMap);
		assertEquals(11000, fundWithNetProfit.netProfit().intValue());
		assertEquals(75000, fundWithNetProfit.gross().intValue());
	}
}
