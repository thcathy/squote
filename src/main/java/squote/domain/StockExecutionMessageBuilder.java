package squote.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import squote.SquoteConstants.Side;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public class StockExecutionMessageBuilder {
	private static Logger log = LoggerFactory.getLogger(StockExecutionMessageBuilder.class);

    public static Optional<StockExecutionMessage> build(String message) {
    	if (isScbFullyFilled(message)) {
			return parseScbMessage(message);
		} else if (isFutuFullyFilled(message)) {
    		return parseFutuMessage(message);
		} else if (isUsmart(message)) {
    		return parseUsmartMessage(message);
		} else if (isMox(message)) {
			return parseMoxMessage(message);
		}
    	
    	return Optional.empty();
    }

	private static boolean isMox(String message) {
		return message.startsWith("Mox") && message.contains("剩餘");
	}

	private static boolean isScbFullyFilled(String message) {
		return message.startsWith("渣打") && message.contains("已完成");
	}

	private static boolean isFutuFullyFilled(String message) {
		return message.contains("富途證券") && message.contains("成交");
	}

	private static boolean isUsmart(String message) {
    	return (message.contains("智能訂單") || message.toLowerCase().contains("usmart")) && (message.contains("已成交") || message.contains("已成功"));
	}

	private static Optional<StockExecutionMessage> parseFutuMessage(String message) {
		int startPos, endPos;
		StockExecutionMessage seMsg = new StockExecutionMessage();

		// parse side
		seMsg.side = Arrays.stream(Side.values()).filter(x-> message.contains(x.chinese)).findFirst().get();

		// parse qty
		startPos = message.indexOf(seMsg.side.chinese) + 2;
		endPos = message.indexOf("股");
		seMsg.quantity = Integer.parseInt(message.substring(startPos, endPos).replaceAll(",", ""));

		// parse code
		startPos = endPos + 1;
		startPos = message.indexOf(".HK)", startPos) - 4;
		endPos = message.indexOf(".HK", startPos);
		seMsg.code = message.substring(startPos, endPos).replaceFirst("^0+(?!$)", "");

		// parse price
		startPos = message.indexOf("成交價格：", endPos) + 5;
		endPos = message.indexOf("，", startPos);
		seMsg.price = new BigDecimal(message.substring(startPos, endPos).replaceAll("[^0-9.]+", ""));

		// parse exec id
		seMsg.executionId = UUID.randomUUID().toString();

		// parse execution date
		try {
			startPos = message.indexOf("成交，", endPos) + 3;
			endPos = startPos + 10;
			var isHyphen = message.charAt(startPos+4) == '-';
			seMsg.date = new SimpleDateFormat(isHyphen ? "yyyy-MM-dd" : "yyyy/MM/dd").parse(message.substring(startPos, endPos));
		} catch (ParseException e) {
			log.warn("Cannot parse execution date", e);
		}
		seMsg.broker = Broker.FUTU;
		return Optional.of(seMsg);
	}

	private static Optional<StockExecutionMessage> parseUsmartMessage(String message) {
		int startPos, endPos;
		StockExecutionMessage seMsg = new StockExecutionMessage();

		// parse side
		seMsg.side = Arrays.stream(Side.values()).filter(x-> message.contains(x.chinese)).findFirst().get();

		// parse code
		startPos = message.indexOf(seMsg.side.chinese) + 2;
		endPos = startPos+5;
		seMsg.code = message.substring(startPos, endPos).replaceFirst("^0+(?!$)", "");

		// parse qty
		startPos = endPos + 1;
		if (message.indexOf("數量") >= 0) {
			startPos = message.indexOf("數量") + 2;
			endPos = message.indexOf("股");
			seMsg.quantity = Integer.parseInt(message.substring(startPos, endPos).replaceAll(",", ""));
		} else {
			endPos = message.indexOf("股");
			seMsg.quantity = Integer.parseInt(message.substring(startPos, endPos).replaceAll("[\\D]", ""));
		}

		// parse price
		if (message.contains("成交價格")) {
			startPos = message.indexOf("成交價格", endPos) + 4;
			endPos = message.indexOf("港幣", startPos);
			seMsg.price = new BigDecimal(message.substring(startPos, endPos).replaceAll("[^0-9.]+", ""));
		} else if (message.contains("成交均價格")) {
			startPos = message.indexOf("成交均價格", endPos) + 5;
			endPos = message.indexOf("港幣", startPos);
			seMsg.price = new BigDecimal(message.substring(startPos, endPos).replaceAll("[^0-9.]+", ""));
		} else if (message.contains("成交價")) {
			startPos = message.indexOf("成交價", endPos) + 3;
			endPos = message.indexOf("港幣", startPos);
			seMsg.price = new BigDecimal(message.substring(startPos, endPos).replaceAll("[^0-9.]+", ""));
		}

		seMsg.executionId = UUID.randomUUID().toString();
		seMsg.date = new Date();
		seMsg.broker = Broker.USMART;
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
		seMsg.quantity = Integer.parseInt(message.substring(startPos, endPos).replaceAll(",", ""));

		// parse code
		startPos = endPos + 1;
		endPos = message.indexOf(".", startPos);
		seMsg.code = message.substring(startPos, endPos).replaceFirst("^0+(?!$)", "");

		// parse price
		startPos = message.indexOf("平均價", endPos) + 6;
		endPos = message.indexOf("\n", startPos);
		seMsg.price = new BigDecimal(message.substring(startPos, endPos).replaceAll("[^0-9.]+", ""));

		// parse exec id
		seMsg.executionId = message.split("\n")[3];

		seMsg.date = new Date();
		seMsg.broker = Broker.SCBANK;
		return Optional.of(seMsg);
	}

	private static Optional<StockExecutionMessage> parseMoxMessage(String message) {
		int startPos, endPos;
		StockExecutionMessage executionMessage = new StockExecutionMessage();

		// parse side
		executionMessage.side = Arrays.stream(Side.values()).filter(x-> message.contains(x.chinese)).findFirst().get();

		// parse qty
		startPos = message.indexOf(executionMessage.side.chinese) + 2;
		endPos = message.indexOf("股");
		executionMessage.quantity = Integer.parseInt(message.substring(startPos, endPos).replaceAll(",", ""));

		// parse code
		startPos = endPos + 1;
		endPos = message.indexOf(".HK", startPos);
		executionMessage.code = message.substring(startPos, endPos).replaceFirst("^0+(?!$)", "");

		// parse price
		startPos = message.indexOf("成交價HKD", endPos) + 6;
		endPos = message.indexOf("。", startPos);
		executionMessage.price = new BigDecimal(message.substring(startPos, endPos).replaceAll("[^0-9.]+", ""));

		// parse exec id
		startPos = message.indexOf("訂單編號：", endPos) + 5;
		executionMessage.executionId = message.substring(startPos);

        try {
            executionMessage.date = new SimpleDateFormat("yyyyMMdd").parse(executionMessage.executionId.split("-")[0]);
        } catch (ParseException e) {
			log.warn("Cannot parse execution date", e);
        }
        executionMessage.broker = Broker.MOX;
		return Optional.of(executionMessage);
	}


}
