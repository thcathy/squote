package squote.domain;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.data.annotation.Id;
import org.springframework.format.annotation.DateTimeFormat;

public class Fund {
	@Id
    private final String name;
			
	private final @DateTimeFormat(pattern="yyyy-MM-dd") Date date;
	private final Map<String, FundHolding> holdings = new ConcurrentHashMap<>();
	
	public Fund(String name) {
		this.name = name;
		this.date = new Date();
	}
		
	public void buyStock(String code, int qty, BigDecimal gross) {		
		if (holdings.containsKey(code)) {
			holdings.put(code, increaseHolding(holdings.get(code), qty, gross));
		} else {
			holdings.put(code, FundHolding.create(code, qty, gross));
		}
	}
		
	public void sellStock(String code, int qty, BigDecimal gross) {
		if (!holdings.containsKey(code)) throw new IllegalArgumentException("Fund does not hold " + code);
		
		FundHolding updatedHolding = decreaseHolding(holdings.get(code), qty, gross);
		if (updatedHolding.getQuantity() <=0) 
			holdings.remove(code);
		else
			holdings.put(code, updatedHolding);		
	}

	@Override
	public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
		
	public Date getDate() { return this.date; }
	public Map<String, FundHolding> getHoldings() { return holdings; }
	
	private FundHolding increaseHolding(FundHolding fundHolding, int qty, BigDecimal gross) {		
		return FundHolding.create(fundHolding.getCode(), fundHolding.getQuantity() + qty, gross.add(fundHolding.getGross()));
	}
	
	private FundHolding decreaseHolding(FundHolding fundHolding, int qty,
			BigDecimal gross) {		
		return FundHolding.create(fundHolding.getCode(), fundHolding.getQuantity() - qty, fundHolding.getGross().subtract(gross));
	}
}
