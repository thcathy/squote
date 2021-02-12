package squote.domain;


import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FundTest {
	public static String FUND_NAME = "Winner";
		
	private Fund createSimpleFund() {
		Fund f = new Fund("tester", FUND_NAME);
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
	
	@Test
	public void interestPayment_givenInterest_ShouldSubstractFromHolding() {
		Fund f = createSimpleFund();
		FundHolding returnHolding = f.payInterest("2800", new BigDecimal("80.5"));
		FundHolding selectedHolding = f.getHoldings().get("2800");
		
		assertEquals(1000, selectedHolding.getQuantity());
		assertEquals(1000, returnHolding.getQuantity());
		
		assertEquals(new BigDecimal("24919.5"), selectedHolding.getGross());
		assertEquals(new BigDecimal("24919.5"), returnHolding.getGross());
		
		assertEquals(new BigDecimal("24.9195"), selectedHolding.getPrice());
		assertEquals(new BigDecimal("24.9195"), returnHolding.getPrice());		
	}

	@Test
	public void cashOut_givenValue_ShouldAddToAmount() {
		Fund f = createSimpleFund();
		f.cashout(new BigDecimal("234.1"));
		assertEquals(new BigDecimal("234.1"), f.getCashoutAmount());
	}
}
