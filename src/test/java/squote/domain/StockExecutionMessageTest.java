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
		
		StockExecutionMessage msg = StockExecutionMessageBuilder.build(scbBuyMsg).get();
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

		StockExecutionMessage msg = StockExecutionMessageBuilder.build(scbSellMsg).get();
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
		assertTrue(!StockExecutionMessageBuilder.build(notCompleteMsg).isPresent());
	}
	
	@Test
	public void construct_GivenUnknownMsg_ShouldReturnAbsent() {
		Optional<StockExecutionMessage> msg = StockExecutionMessageBuilder.build("asdlkflwe");
		assertTrue(!msg.isPresent());
	}

	@Test
	public void construct_GivenFutuSellMsg_ShouldParseSuccess() {
		String futuSellMsg = "【成交提醒】成功賣出300股政府債券二四零六(04246.HK)，成交價格：103.100，此筆訂單委託已全部成交，2021-06-24 13:00:05。 【富途證券(香港)】";
		StockExecutionMessage msg = StockExecutionMessageBuilder.build(futuSellMsg).get();
		assertEquals(SquoteConstants.Side.SELL, msg.getSide());
		assertEquals(300, msg.getQuantity());
		assertEquals("4246", msg.getCode());
		assertEquals(new BigDecimal("103.100"), msg.getPrice());
		assertEquals("2021-06-24", new SimpleDateFormat("yyyy-MM-dd").format(msg.getDate()));
		assertEquals(19.89, msg.fees.get(""));
		assertEquals(19.89 + 40.21, msg.fees.get(""));
	}

	@Test
	public void construct_GivenFutuBuyMsg_ShouldParseSuccess() {
		String futuSellMsg = "【成交提醒】成功買入7,200股南方两倍看多國指(07288.HK)，成交價格：6.855，此筆訂單委託已全部成交，2021-07-07 13:22:05。 【富途證券(香港)】";
		StockExecutionMessage msg = StockExecutionMessageBuilder.build(futuSellMsg).get();
		assertEquals(SquoteConstants.Side.BUY, msg.getSide());
		assertEquals(7200, msg.getQuantity());
		assertEquals("7288", msg.getCode());
		assertEquals(new BigDecimal("6.855"), msg.getPrice());
		assertEquals("2021-07-07", new SimpleDateFormat("yyyy-MM-dd").format(msg.getDate()));
	}
}
