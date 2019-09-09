package squote.domain;
import java.math.BigInteger;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.data.annotation.Id;

public class StockQuery {
	@Id
    private BigInteger id;
	
	private int key;

	private String delimitedStocks;
	
	public StockQuery(String delimitedStocks) { this.delimitedStocks = delimitedStocks; }

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

	public String getDelimitedStocks() {
        return this.delimitedStocks;
    }

	public void setDelimitedStocks(String delimitedStocks) {
        this.delimitedStocks = delimitedStocks;
    }
}
