package squote.domain;
import java.math.BigDecimal;
import java.util.Date;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.format.annotation.DateTimeFormat;

public class FundHolding {	
	private final String code;
	private final int quantity;
	private final BigDecimal gross;
	private final @DateTimeFormat(pattern="yyyy-MM-dd") Date date;
		
	public FundHolding(String code, int quantity, BigDecimal gross, Date date) {
		super();
		this.code = code;
		this.quantity = quantity;
		this.gross = gross;
		this.date = date;
	}
	
	public BigDecimal getPrice() { return gross.divide(BigDecimal.valueOf(quantity)); }
		
	@Override
	public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

	public String getCode() { return this.code; }
	public int getQuantity() { return this.quantity;}
	public BigDecimal getGross() { return this.gross; }
	public Date getDate() { return this.date; }

	public static FundHolding create(String code, int qty, BigDecimal gross) {		
		return new FundHolding(code, qty, gross, new Date());
	}	
}
