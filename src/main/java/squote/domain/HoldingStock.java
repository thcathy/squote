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
	
	private String code;
	private int quantity;
	private BigDecimal gross;
	@DateTimeFormat(pattern="yyyy-MM-dd") private Date date;
	private BigDecimal hsce;
	private SquoteConstants.Side side;
	
	public BigDecimal getPrice() { return gross.divide(BigDecimal.valueOf(quantity)); }
	
	public BigInteger getId() { return this.id; }
	public void setId(BigInteger id) { this.id = id; }

	public String getCode() {
        return this.code;
    }

	public void setCode(String code) {
        this.code = code;
    }

	public int getQuantity() {
        return this.quantity;
    }

	public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

	public BigDecimal getGross() {
        return this.gross;
    }

	public void setGross(BigDecimal gross) {
        this.gross = gross;
    }

	public Date getDate() {
        return this.date;
    }

	public void setDate(Date date) {
        this.date = date;
    }

	public BigDecimal getHsce() {
        return this.hsce;
    }

	public void setHsce(BigDecimal hsce) {
        this.hsce = hsce;
    }

	public Side getSide() {
        return this.side;
    }

	public void setSide(Side side) {
        this.side = side;
    }
	
	@Override
	public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
