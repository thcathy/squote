package squote.domain;
import java.math.BigInteger;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.data.annotation.Id;

public class StockQuery {
	@Id
    private BigInteger id;
	
	private int key;
	
	@NotNull
	private String stockList;
	
	public StockQuery(String stockList) { this.stockList = stockList; }

	@Override
	public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

	public int getKey() {
        return this.key;
    }

	public void setKey(int key) {
        this.key = key;
    }

	public String getStockList() {
        return this.stockList;
    }

	public void setStockList(String stockList) {
        this.stockList = stockList;
    }
}
