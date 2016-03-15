package squote.domain;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.data.annotation.Id;
import org.springframework.format.annotation.DateTimeFormat;

import squote.SquoteConstants;
import squote.SquoteConstants.Side;

public class HoldingStock {
	@Id
    private BigInteger id;
	
	private final String code;
	private final int quantity;
	private final BigDecimal gross;
	private final @DateTimeFormat(pattern="yyyy-MM-dd") Date date;
	private final BigDecimal hsce;
	private final SquoteConstants.Side side;
	
	public HoldingStock(String code, Side side, int quantity, BigDecimal gross, Date date, BigDecimal hsce) {
		super();
		this.code = code;
		this.quantity = quantity;
		this.gross = gross;
		this.date = date;
		this.hsce = hsce;
		this.side = side;
	}
	
	public HoldingStock(BigInteger id, String code, Side side, int quantity, BigDecimal gross, Date date, BigDecimal hsce) {		
		this(code, side, quantity, gross, date, hsce);
		this.id = id;
	}

	public static HoldingStock from(StockExecutionMessage message, BigDecimal hscei) {
		return new HoldingStock(
				message.getCode(), 
				message.getSide(), 
				message.getQuantity(), 
				message.getPrice().multiply(new BigDecimal(message.getQuantity())), 
				message.getDate(), 
				hscei);
	}
	
	public BigDecimal getPrice() { return gross.divide(BigDecimal.valueOf(quantity)); }
	
	public double performance(double latestPrice) {
		return (latestPrice - getPrice().doubleValue())/getPrice().doubleValue()*100;		
	}
	
	public double hscePerformance(double latestHsce) {
		return (latestHsce - hsce.doubleValue())/hsce.doubleValue()*100;
	}
	
	public double relativePerformance(double latestPrice, double latestHsce) {
		return (performance(latestPrice) - hscePerformance(latestHsce)) * side.factor; 
	}
	
	@Override
	public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
		
	public BigInteger getId() { return this.id; }
	public String getCode() { return this.code; }
	public int getQuantity() { return this.quantity;}
	public BigDecimal getGross() { return this.gross; }
	public Date getDate() { return this.date; }
	public BigDecimal getHsce() { return this.hsce; }
	public Side getSide() { return this.side; }
	
	
}
