package squote.domain;

import org.junit.jupiter.api.Test;
import squote.SquoteConstants.Side;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HoldingStockTest {
	static String userId = "User1";

	@Test
	public void relativePerformance_GivenLatestStockAndIndexPrice_ShouldCalculateCorrectPerformance() {
		final String stockCode = "1";
		final BigDecimal stockOriginalPrice = BigDecimal.valueOf(10);
		final BigDecimal hsceiOriginalPrice = BigDecimal.valueOf(100);
		final double stockLatestPrice = 15;
		final double hsceiLatestPrice = 110;
		
		HoldingStock holdingStock = new HoldingStock(userId, stockCode, Side.BUY, 1, stockOriginalPrice, new Date(), hsceiOriginalPrice);
		assertEquals(40, holdingStock.relativePerformance(stockLatestPrice, hsceiLatestPrice), 0.000001);
		
		holdingStock = new HoldingStock(userId, stockCode, Side.SELL, 1, stockOriginalPrice, new Date(), hsceiOriginalPrice);
		assertEquals(-40, holdingStock.relativePerformance(stockLatestPrice, hsceiLatestPrice), 0.000001);
	}
	
	@Test
	public void getGross_GivenGrossWithoutDecimal_ShouldShowCorrectFmt() {
		String scbSellMsg = "渣打:買入6000股883.HK 中國海洋石油\n";
		scbSellMsg += "已完成\n";
		scbSellMsg += "平均價HKD123\n";
		scbSellMsg += "O1512110016740"; 
		
		StockExecutionMessage msg = StockExecutionMessageBuilder.build(scbSellMsg).get();
		HoldingStock holdingStock = HoldingStock.from(msg, userId, new BigDecimal("123"));
		assertEquals(new BigDecimal("738000"), holdingStock.getGross());
	}
}
