package squote.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;

import org.junit.Test;

import squote.SquoteConstants;

import com.google.common.base.Optional;

public class StockExecutionMessageTest {
	
	@Test
	public void constructFromScbMessage() {
		String scbSellMsg = "渣打: (沽出10,000股01138.中海發展股份) \n";
		scbSellMsg += "已於4.8900元成功執行\n";
		scbSellMsg += "20140610000013235";
		
		StockExecutionMessage msg = StockExecutionMessage.construct(scbSellMsg).get();
		assertEquals(SquoteConstants.Side.SELL, msg.getSide());
		assertEquals(10000, msg.getQuantity());
		assertEquals("1138", msg.getCode());
		assertEquals(4.89, msg.getPrice(), 0.00001);
		assertEquals("20140610000013235", msg.getExecutionId());
		assertEquals("2014-06-10", new SimpleDateFormat("yyyy-MM-dd").format(msg.getDate()));
	
		String scbBuyMsg = "渣打: (買入10,000股01138.中海發展股份)\n";
		scbBuyMsg += "已於4.0500元成功執行\n";
		scbBuyMsg += "20140509000023378";
		msg = StockExecutionMessage.construct(scbBuyMsg).get();
		assertEquals(SquoteConstants.Side.BUY, msg.getSide());
		assertEquals(10000, msg.getQuantity());
		assertEquals("1138", msg.getCode());
		assertEquals(4.05, msg.getPrice(), 0.00001);
		assertEquals("20140509000023378", msg.getExecutionId());
		assertEquals("2014-05-09", new SimpleDateFormat("yyyy-MM-dd").format(msg.getDate()));
		
		String scbBuyMsg1880 = "渣打: (沽出5,000股01880.百麗國際)\n";
		scbBuyMsg1880 += "已於8.4200元成功執行\n";
		scbBuyMsg1880 += "20140625000023727";
		msg = StockExecutionMessage.construct(scbBuyMsg1880).get();
		assertEquals(SquoteConstants.Side.SELL, msg.getSide());
		assertEquals(5000, msg.getQuantity());
		assertEquals("1880", msg.getCode());
		assertEquals(8.42, msg.getPrice(), 0.00001);
		assertEquals("20140625000023727", msg.getExecutionId());
		assertEquals("2014-06-25", new SimpleDateFormat("yyyy-MM-dd").format(msg.getDate()));
	}
	
	@Test
	public void constructFromNotCompletedScbMessageShouldReturnAbsent() {
		String notCompleteMsg = "渣打: (買入10,000股01138.中海發展股份)\n";
		notCompleteMsg += "2,000股成交於4.0500\n";
		notCompleteMsg += "20140509000023378";
		assertTrue(!StockExecutionMessage.construct(notCompleteMsg).isPresent());
	}
	
	@Test
	public void constructFromUnknownMsgShouldReturnAbsent() {
		Optional<StockExecutionMessage> msg = StockExecutionMessage.construct("asdlkflwe");
		assertTrue(!msg.isPresent());
	}
}
