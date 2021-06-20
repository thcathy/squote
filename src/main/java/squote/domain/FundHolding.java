package squote.domain;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.data.annotation.Transient;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

public class FundHolding {	
	private final String code;
	private final BigDecimal quantity;
	private final BigDecimal gross;
	private final @DateTimeFormat(pattern="yyyy-MM-dd") Date date;
	
	@Transient
	private BigDecimal spotPrice;
	@Transient
	private BigDecimal netProfit;
		
	public FundHolding(String code, BigDecimal quantity, BigDecimal gross, Date date) {
		super();
		this.code = code;
		this.quantity = quantity;
		this.gross = gross;
		this.date = date;
	}
		
	public static FundHolding create(String code, BigDecimal qty, BigDecimal gross) {
		return new FundHolding(code, qty, gross, new Date());
	}
					
	@Override
	public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
	
	public void calculateNetProfit(BigDecimal spotPrice) {
		if (spotPrice == null) throw new IllegalArgumentException("Spot Price cannot null");
		this.spotPrice = spotPrice;
		this.netProfit = spotPrice.multiply(quantity).subtract(gross);
	}

	public BigDecimal getPrice() {
		if (quantity.compareTo(BigDecimal.ONE) < 0)
			return BigDecimal.ZERO;
		else
			return gross.divide(quantity, 4, RoundingMode.HALF_UP);
	}
	public String getCode() { return this.code; }
	public BigDecimal getQuantity() { return this.quantity;}
	public BigDecimal getGross() { return this.gross; }
	public Date getDate() { return this.date; }
	public BigDecimal netProfit() {
		return netProfit != null ? this.netProfit : BigDecimal.ZERO;
	}
	public BigDecimal getNetProfit() {
		return this.netProfit;
	}
	public BigDecimal spotPrice() {
		if (spotPrice == null) throw new IllegalStateException("Spot Price hadn't calculated");
		return this.spotPrice; 
	}
	
}
