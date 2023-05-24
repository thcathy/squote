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
		String futuBuyMsg = "【成交提醒】成功買入7,200股南方两倍看多國指(07288.HK)，成交價格：6.855，此筆訂單委託已全部成交，2021-07-07 13:22:05。 【富途證券(香港)】";
		StockExecutionMessage msg = StockExecutionMessageBuilder.build(futuBuyMsg).get();
		assertEquals(SquoteConstants.Side.BUY, msg.getSide());
		assertEquals(7200, msg.getQuantity());
		assertEquals("7288", msg.getCode());
		assertEquals(new BigDecimal("6.855"), msg.getPrice());
		assertEquals("2021-07-07", new SimpleDateFormat("yyyy-MM-dd").format(msg.getDate()));
	}

	@Test
	public void construct_GivenFutuPartialSellMsg_ShouldParseSuccess() {
		String futuBuyMsg = "【成交提醒】成功賣出6,500股$南方两倍看多國指(07288.HK)$，成交價格：5.005，此筆訂單委託還剩下9,500股待成交，2022-02-10 09:21:10。【富途證券(香港)】";
		StockExecutionMessage msg = StockExecutionMessageBuilder.build(futuBuyMsg).get();
		assertEquals(SquoteConstants.Side.SELL, msg.getSide());
		assertEquals(6500, msg.getQuantity());
		assertEquals("7288", msg.getCode());
		assertEquals(new BigDecimal("5.005"), msg.getPrice());
		assertEquals("2022-02-10", new SimpleDateFormat("yyyy-MM-dd").format(msg.getDate()));
	}

	@Test
	public void construct_GivenUsmartBuyMsg_ShouldParseSuccess() {
		String usmartBuyMsg = "尊敬的客戶，您所委托的智能訂單已成交：買入07288FL二南方國指，數量19,000股，成交價格4.490港幣。";
		StockExecutionMessage msg = StockExecutionMessageBuilder.build(usmartBuyMsg).get();
		assertEquals(SquoteConstants.Side.BUY, msg.getSide());
		assertEquals(19000, msg.getQuantity());
		assertEquals("7288", msg.getCode());
		assertEquals(new BigDecimal("4.490"), msg.getPrice());
		assertEquals(new SimpleDateFormat("yyyy-MM-dd").format(new Date()), new SimpleDateFormat("yyyy-MM-dd").format(msg.getDate()));
	}

	@Test
	public void construct_GivenUsmartBuyMsg2_ShouldParseSuccess() {
		String usmartBuyMsg = "您已成功買入07288FL二南方國指，數量20,000股，成交均價格4.414港幣，買入金額88280.00港幣。詳情請登錄uSMART APP查看，感謝您的支持";
		StockExecutionMessage msg = StockExecutionMessageBuilder.build(usmartBuyMsg).get();
		assertEquals(SquoteConstants.Side.BUY, msg.getSide());
		assertEquals(20000, msg.getQuantity());
		assertEquals("7288", msg.getCode());
		assertEquals(new BigDecimal("4.414"), msg.getPrice());
		assertEquals(new SimpleDateFormat("yyyy-MM-dd").format(new Date()), new SimpleDateFormat("yyyy-MM-dd").format(msg.getDate()));
	}

	@Test
	public void construct_GivenUsmartSellMsg_ShouldParseSuccess() {
		String usmartSellMsg = "您已成功賣出07288南方兩倍做多國指ETF36,200股，成交價2.930港幣。 usmart";
		StockExecutionMessage msg = StockExecutionMessageBuilder.build(usmartSellMsg).get();
		assertEquals(SquoteConstants.Side.SELL, msg.getSide());
		assertEquals(36200, msg.getQuantity());
		assertEquals("7288", msg.getCode());
		assertEquals(new BigDecimal("2.930"), msg.getPrice());
		assertEquals(new SimpleDateFormat("yyyy-MM-dd").format(new Date()), new SimpleDateFormat("yyyy-MM-dd").format(msg.getDate()));
	}

	@Test
	public void construct_GivenFutuMsgWithParentheses_ShouldParseSuccess() {
		String futuMsg = "成交提醒 【成交提醒】成功買入14,800股$南方東英恒生指數每日槓桿(2x)產品 (07200.HK)$，成交價格：4.056，此筆訂單委託已全部成交，2023-05-24 09:30:59（香港時間）。【富途證券(香港)】";
		StockExecutionMessage msg = StockExecutionMessageBuilder.build(futuMsg).get();
		assertEquals(SquoteConstants.Side.BUY, msg.getSide());
		assertEquals(14800, msg.getQuantity());
		assertEquals("7200", msg.getCode());
		assertEquals(new BigDecimal("4.056"), msg.getPrice());
		assertEquals("2023-05-24", new SimpleDateFormat("yyyy-MM-dd").format(msg.getDate()));
	}
}
