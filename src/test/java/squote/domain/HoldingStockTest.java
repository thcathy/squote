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
}
