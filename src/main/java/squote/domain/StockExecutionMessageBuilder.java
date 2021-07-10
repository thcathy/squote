package squote.domain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import squote.SquoteConstants.Side;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

public class StockExecutionMessageBuilder {
	private static Logger log = LoggerFactory.getLogger(StockExecutionMessageBuilder.class);

    public static Optional<StockExecutionMessage> build(String message) {
    	if (isScbFullyFilled(message)) {
			return parseScbMessage(message);
		} else if (isFutuFullyFilled(message)) {
    		return parseFutuMessage(message);
		}
    	
    	return Optional.empty();
    }

	private static boolean isScbFullyFilled(String message) {
		return message.startsWith("渣打") && message.contains("已完成");
	}

	private static boolean isFutuFullyFilled(String message) {
		return message.contains("富途證券") && message.contains("全部成交");
	}

	private static Optional<StockExecutionMessage> parseFutuMessage(String message) {
		int startPos, endPos;
		StockExecutionMessage seMsg = new StockExecutionMessage();

		// parse side
		seMsg.side = Arrays.stream(Side.values()).filter(x-> message.contains(x.chinese)).findFirst().get();

		// parse qty
		startPos = message.indexOf(seMsg.side.chinese) + 2;
		endPos = message.indexOf("股");
		seMsg.quantity = Integer.valueOf(message.substring(startPos, endPos).replaceAll(",", ""));

		// parse code
		startPos = endPos + 1;
		startPos = message.indexOf("(", startPos) + 1;
		endPos = message.indexOf(".HK", startPos);
		seMsg.code = message.substring(startPos, endPos).replaceFirst("^0+(?!$)", "");

		// parse price
		startPos = message.indexOf("成交價格：", endPos) + 5;
		endPos = message.indexOf("，", startPos);
		seMsg.price = new BigDecimal(message.substring(startPos, endPos).replaceAll("[^0-9\\.]+", ""));

		// parse exec id
		seMsg.executionId = UUID.randomUUID().toString();

		// parse execution date
		try {
			startPos = message.indexOf("全部成交，", endPos) + 5;
			endPos = startPos + 10;
			seMsg.date = new SimpleDateFormat("yyyy-MM-dd").parse(message.substring(startPos, endPos));
		} catch (ParseException e) {
			log.warn("Cannot parse execution date", e);
		}
		seMsg.broker = Broker.FUTU;
		return Optional.of(seMsg);
	}

	private static Optional<StockExecutionMessage> parseScbMessage(String message) {
		int startPos, endPos;
		StockExecutionMessage seMsg = new StockExecutionMessage();

		// parse side
		seMsg.side = Arrays.stream(Side.values()).filter(x-> message.contains(x.chinese)).findFirst().get();

		// parse qty
		startPos = message.indexOf(seMsg.side.chinese) + 2;
		endPos = message.indexOf("股");
		seMsg.quantity = Integer.valueOf(message.substring(startPos, endPos).replaceAll(",", ""));

		// parse code
		startPos = endPos + 1;
		endPos = message.indexOf(".", startPos);
		seMsg.code = message.substring(startPos, endPos).replaceFirst("^0+(?!$)", "");

		// parse price
		startPos = message.indexOf("平均價", endPos) + 6;
		endPos = message.indexOf("\n", startPos);
		seMsg.price = new BigDecimal(message.substring(startPos, endPos).replaceAll("[^0-9\\.]+", ""));

		// parse exec id
		seMsg.executionId = message.split("\n")[3];

		// parse execution date
		try {
			seMsg.date = new SimpleDateFormat("yyMMdd").parse(seMsg.executionId.substring(1, 7));
		} catch (ParseException e) {
			log.warn("Cannot parse execution date", e);
		}
		seMsg.broker = Broker.SCBANK;
		return Optional.of(seMsg);
	}


}
