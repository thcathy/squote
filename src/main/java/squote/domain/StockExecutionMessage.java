package squote.domain;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import squote.SquoteConstants.Side;

public class StockExecutionMessage {
	private static Logger log = LoggerFactory.getLogger(StockExecutionMessage.class);

    private String executionId;
    private String code;
    private Side side;
    private double price;
    private int quantity;
    private Date date;
    
    public static Optional<StockExecutionMessage> construct(String message) {
    	if (message.startsWith("渣打") && message.contains("成功執行")) {
    		int startPos, endPos;
    		StockExecutionMessage seMsg = new StockExecutionMessage();
    	
    		// parse side
    		seMsg.side = Arrays.stream(Side.values()).filter(x->message.contains(x.chinese)).findFirst().get();
    		
    		// parse qty
    		startPos = message.indexOf(seMsg.side.chinese) + 2;
    		endPos = message.indexOf("股");
    		seMsg.quantity = Integer.valueOf(message.substring(startPos, endPos).replaceAll(",", ""));
    		
    		// parse code
    		startPos = endPos + 2;
    		endPos = message.indexOf(".", startPos);
    		seMsg.code = message.substring(startPos, endPos).replaceFirst("^0+(?!$)", "");
    		
    		// parse price
    		startPos = message.indexOf("已於", endPos) + 2;
    		endPos = message.indexOf("元", startPos);
    		seMsg.price = Double.valueOf(message.substring(startPos, endPos));
    		
    		// parse exec id
    		seMsg.executionId = message.split("\n")[2];
    		
    		// parse execution date
    		try {
				seMsg.date = new SimpleDateFormat("yyyyMMdd").parse(seMsg.executionId.substring(0, 8));
			} catch (ParseException e) {
				log.warn("Cannot parse execution date", e);				
			}
    		
    		return Optional.of(seMsg);
    	}
    	
    	return Optional.empty();
    }
    

	public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

	public String getExecutionId() {
        return this.executionId;
    }

	public String getCode() {
        return this.code;
    }

	public Side getSide() {
        return this.side;
    }

	public double getPrice() {
        return this.price;
    }

	public int getQuantity() {
        return this.quantity;
    }

	public Date getDate() {
        return this.date;
    }
}
