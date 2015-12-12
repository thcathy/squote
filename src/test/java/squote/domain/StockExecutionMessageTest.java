package squote.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Optional;

import org.junit.Test;

import squote.SquoteConstants;

public class StockExecutionMessageTest {
	
	@Test
	public void contstruct_GivenRightExeMsg_ShouldParseSuccess() {
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
	public void construct_GivenNotCompletedMsg_ShouldReturnAbsent() {
		String notCompleteMsg = "渣打: (買入10,000股01138.中海發展股份)\n";
		notCompleteMsg += "2,000股成交於4.0500\n";
		notCompleteMsg += "20140509000023378";
		assertTrue(!StockExecutionMessage.construct(notCompleteMsg).isPresent());
	}
	
	@Test
	public void construct_GivenUnknownMsg_ShouldReturnAbsent() {
		Optional<StockExecutionMessage> msg = StockExecutionMessage.construct("asdlkflwe");
		assertTrue(!msg.isPresent());
	}
}
