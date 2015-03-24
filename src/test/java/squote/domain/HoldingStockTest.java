package squote.domain;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.Date;

import org.junit.Test;

import squote.SquoteConstants.Side;

public class HoldingStockTest {
	@Test
	public void relativePerformance_GivenLatestStockAndIndexPrice_ShouldCalculateCorrectPerformance() {
		final String stockCode = "1";
		final BigDecimal stockOriginalPrice = BigDecimal.valueOf(10);
		final BigDecimal hsceiOriginalPrice = BigDecimal.valueOf(100);
		final double stockLatestPrice = 15;
		final double hsceiLatestPrice = 110;
		
		HoldingStock holdingStock = new HoldingStock(stockCode, Side.BUY, 1, stockOriginalPrice, new Date(), hsceiOriginalPrice);
		assertEquals(40, holdingStock.relativePerformance(stockLatestPrice, hsceiLatestPrice), 0.000001);
		
		holdingStock = new HoldingStock(stockCode, Side.SELL, 1, stockOriginalPrice, new Date(), hsceiOriginalPrice);
		assertEquals(-40, holdingStock.relativePerformance(stockLatestPrice, hsceiLatestPrice), 0.000001);
	}
	
	@Test
	public void getGross_GivenGrossWithoutDecimal_ShouldShowCorrectFmt() {
		String scbBuyMsg = "渣打: (買入10,000股01138.中海發展股份) \n";
		scbBuyMsg += "已於5.2300元成功執行\n";
		scbBuyMsg += "20150205000020865";
		
		StockExecutionMessage msg = StockExecutionMessage.construct(scbBuyMsg).get();
		HoldingStock holdingStock = HoldingStock.from(msg, new BigDecimal("123"));
		assertEquals(new BigDecimal("52300.0000"), holdingStock.getGross());
	}
}
