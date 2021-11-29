package squote.domain;

import org.junit.jupiter.api.Test;
import squote.SquoteConstants;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StockExecutionMessageTest {
	
	@Test
	public void construct_GivenRightExeMsg_ShouldParseSuccess() {
		String scbBuyMsg = "渣打:買入17500股7288.HK\n" +
				"已完成\n" +
				"平均價HKD4.79\n" +
				"OSCBABT44566440";
		
		StockExecutionMessage msg = StockExecutionMessageBuilder.build(scbBuyMsg).get();
		assertEquals(SquoteConstants.Side.BUY, msg.getSide());
		assertEquals(17500, msg.getQuantity());
		assertEquals("7288", msg.getCode());
		assertEquals(new BigDecimal("4.79"), msg.getPrice());
		assertEquals("OSCBABT44566440", msg.getExecutionId());

		var dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
		assertEquals(dateFormatter.format(new Date()), dateFormatter.format(msg.getDate()));
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
