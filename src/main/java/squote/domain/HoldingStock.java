package squote.domain;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.format.annotation.DateTimeFormat;
import squote.SquoteConstants;
import squote.SquoteConstants.Side;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class HoldingStock {
	@Id
    private String id;
	
	private String code;

	@Indexed
	private String userId;
	private int quantity;
	private BigDecimal gross;
	private @DateTimeFormat(pattern="yyyy-MM-dd") Date date;
	private BigDecimal hsce;
	private SquoteConstants.Side side;
	private String fundName;
	private String fillIds;

	@Transient
	public Map<String, BigDecimal> fees;
	
	public HoldingStock() {}
	
	public HoldingStock(String userId, String code, Side side, int quantity, BigDecimal gross, Date date, BigDecimal hsce) {
		this(UUID.randomUUID().toString(), userId, code, side, quantity, gross, date, hsce);
	}

	public HoldingStock(String id, String userId, String code, Side side, int quantity, BigDecimal gross, Date date, BigDecimal hsce) {
		super();
		this.id = id;
		this.userId = userId;
		this.code = code;
		this.quantity = quantity;
		this.gross = gross;
		this.date = date;
		this.hsce = hsce;
		this.side = side;
	}

	public static HoldingStock simple(String code, Side side, int quantity, BigDecimal gross, String fundName) {
		var holding = new HoldingStock(null, code, side, quantity, gross, null, null);
		holding.fundName = fundName;
		return holding;
	}

	public static HoldingStock simple(String code, Side side, int quantity, BigDecimal gross, String fundName, Date date) {
		var holding = simple(code, side, quantity, gross, fundName);
		holding.date = date;
		return holding;
	}

	public static HoldingStock from(StockExecutionMessage message, String userId, BigDecimal hscei) {
		return new HoldingStock(
				userId,
				message.getCode(), 
				message.getSide(), 
				message.getQuantity(), 
				message.getPrice().multiply(new BigDecimal(message.getQuantity())), 
				message.getDate(),
				hscei);
	}

	public static HoldingStock from(Execution execution, String userId) {
		var holding = new HoldingStock(
				userId,
				execution.getCode(),
				execution.getSide(),
				execution.getQuantity().intValue(),
				execution.getPrice().multiply(execution.getQuantity()),
				new Date(execution.getTime()),
				null);
		holding.setFillIds(execution.getFillIds());
		return holding;
	}
	
	public BigDecimal getPrice() { 
		return gross.divide(BigDecimal.valueOf(quantity), 4, RoundingMode.HALF_UP);
	}
	
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
		
	public String getId() { return this.id; }
	public String getCode() { return this.code; }
	public int getQuantity() { return this.quantity;}
	public BigDecimal getGross() { return this.gross; }
	public Date getDate() { return this.date; }
	public BigDecimal getHsce() { return this.hsce; }
	public Side getSide() { return this.side; }
	public String getUserId() { return this.userId; }
	public String getFundName() { return fundName; }
	public HoldingStock setFundName(String fundName) { this.fundName = fundName; return this; }
	public String getFillIds() { return fillIds; }
	public HoldingStock setFillIds(String fillIds) { this.fillIds = fillIds; return this; }
}
