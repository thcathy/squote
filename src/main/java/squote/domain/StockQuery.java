package squote.domain;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

import java.math.BigInteger;

public class StockQuery {
	@Id
    private BigInteger id;

	@Indexed
	private String userId;

	private String delimitedStocks;
	
	public StockQuery(String userId, String delimitedStocks) {
	    this.userId = userId;
	    this.delimitedStocks = delimitedStocks;
	}

	@Override
	public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    public String getUserId() {
        return userId;
    }

    public StockQuery setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public String getDelimitedStocks() {
        return delimitedStocks;
    }

    public StockQuery setDelimitedStocks(String delimitedStocks) {
        this.delimitedStocks = delimitedStocks;
        return this;
    }
}
