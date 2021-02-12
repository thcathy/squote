package squote.domain;

import org.junit.jupiter.api.Test;
import squote.SquoteConstants;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StockExecutionMessageTest {
	
	@Test
	public void construct_GivenRightExeMsg_ShouldParseSuccess() {
		String scbBuyMsg = "渣打:買入6000股883.HK 中國海洋石油\n";
		scbBuyMsg += "已完成\n";
		scbBuyMsg += "平均價HKD7.99\n";
		scbBuyMsg += "O1512110016740";
		
		StockExecutionMessage msg = StockExecutionMessage.construct(scbBuyMsg).get();
		assertEquals(SquoteConstants.Side.BUY, msg.getSide());
		assertEquals(6000, msg.getQuantity());
		assertEquals("883", msg.getCode());
		assertEquals(new BigDecimal("7.99"), msg.getPrice());
		assertEquals("O1512110016740", msg.getExecutionId());
		assertEquals("2015-12-11", new SimpleDateFormat("yyyy-MM-dd").format(msg.getDate()));
	}
	
	@Test
	public void construct_givenSellExeMsg_ShouldParseSuccess() {
		String scbSellMsg = "渣打:賣出20000股1138.HK 中海發展股份\n";
		scbSellMsg += "已完成\n";
		scbSellMsg += "平均價HKD5.65\n";
		scbSellMsg += "O1512140014486";

		StockExecutionMessage msg = StockExecutionMessage.construct(scbSellMsg).get();
		assertEquals(SquoteConstants.Side.SELL, msg.getSide());
		assertEquals(20000, msg.getQuantity());
		assertEquals("1138", msg.getCode());
		assertEquals(new BigDecimal("5.65"), msg.getPrice());
		assertEquals("O1512140014486", msg.getExecutionId());
		assertEquals("2015-12-14", new SimpleDateFormat("yyyy-MM-dd").format(msg.getDate()));
	}
	
	@Test
	public void construct_GivenNotCompletedMsg_ShouldReturnAbsent() {
		String notCompleteMsg = "渣打:買入240000股362.HK有效至今天";
		notCompleteMsg += "60000股於HKD0.71成交\n";
		notCompleteMsg += "O1511300012292";
		assertTrue(!StockExecutionMessage.construct(notCompleteMsg).isPresent());
	}
	
	@Test
	public void construct_GivenUnknownMsg_ShouldReturnAbsent() {
		Optional<StockExecutionMessage> msg = StockExecutionMessage.construct("asdlkflwe");
		assertTrue(!msg.isPresent());
	}
}
