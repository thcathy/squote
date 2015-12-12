package squote.domain;

import static org.junit.Assert.assertEquals;

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
		String scbSellMsg = "渣打:買入6000股883.HK 中國海洋石油\n";
		scbSellMsg += "已完成\n";
		scbSellMsg += "平均價HKD123\n";
		scbSellMsg += "O1512110016740"; 
		
		StockExecutionMessage msg = StockExecutionMessage.construct(scbSellMsg).get();
		HoldingStock holdingStock = HoldingStock.from(msg, new BigDecimal("123"));
		assertEquals(new BigDecimal("738000"), holdingStock.getGross());
	}
}
