package squote.domain;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.Date;

import org.junit.Test;

import squote.SquoteConstants.Side;

public class HoldingStockTest {
	@Test
	public void calculateCorrectPerformance() {
		HoldingStock holdingStock = new HoldingStock("1", Side.BUY, 1, BigDecimal.valueOf(10), new Date(), BigDecimal.valueOf(100));
		assertEquals(40, holdingStock.relativePerformance(15, 110), 0.000001);
		
		holdingStock = new HoldingStock("1", Side.SELL, 1, BigDecimal.valueOf(10), new Date(), BigDecimal.valueOf(100));
		assertEquals(-40, holdingStock.relativePerformance(15, 110), 0.000001);
	}
	
	@Test
	public void GrossFormatIsCorrect() {
		String scbBuyMsg = "渣打: (買入10,000股01138.中海發展股份) \n";
		scbBuyMsg += "已於5.2300元成功執行\n";
		scbBuyMsg += "20150205000020865";
		
		StockExecutionMessage msg = StockExecutionMessage.construct(scbBuyMsg).get();
		HoldingStock holdingStock = HoldingStock.from(msg, new BigDecimal("123"));
		assertEquals(new BigDecimal("52300.0000"), holdingStock.getGross());
	}
}
