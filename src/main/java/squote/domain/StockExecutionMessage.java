package squote.domain;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import squote.SquoteConstants.Side;

import java.math.BigDecimal;
import java.util.Date;

public class StockExecutionMessage {
	private static Logger log = LoggerFactory.getLogger(StockExecutionMessage.class);
    public String executionId;
	public String code;
	public Side side;
	public BigDecimal price;
	public int quantity;
	public Date date;
	public Broker broker;

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

	public BigDecimal getPrice() {
        return this.price;
    }

	public int getQuantity() {
        return this.quantity;
    }

	public Date getDate() {
        return this.date;
    }
}
