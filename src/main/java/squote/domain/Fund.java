package squote.domain;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Fund {
	private static Logger log = LoggerFactory.getLogger(Fund.class);

	public enum FundType {
		STOCK, CRYPTO
	}

	@Id
	private String id;

	public final String name;

	@Indexed
	public String userId;
	private @DateTimeFormat(pattern="yyyy-MM-dd") Date date;
	private Map<String, FundHolding> holdings = new ConcurrentHashMap<>();
	private BigDecimal profit = new BigDecimal("0");
	private BigDecimal netProfit = new BigDecimal("0");
	private BigDecimal cashoutAmount = new BigDecimal("0");
	private BigDecimal cashinAmount = new BigDecimal("0");
	private FundType type;

	public Fund(String userId, String name) {
		this.userId = userId;
		this.name = name;
		this.date = new Date();
		this.type = FundType.STOCK;
	}

	public Fund cashout(BigDecimal value) {
		cashoutAmount = cashoutAmount.add(value);
		return this;
	}

	public Fund cashin(BigDecimal value) {
		cashinAmount = cashinAmount.add(value);
		return this;
	}

	public void buyStock(String code, BigDecimal qty, BigDecimal gross) {
		if (holdings.containsKey(code)) {
			holdings.put(code, increaseHolding(holdings.get(code), qty, gross));
		} else {
			holdings.put(code, FundHolding.create(code, qty, gross));
		}
	}

	public void sellStock(String code, BigDecimal qty, BigDecimal gross) {
		if (!holdings.containsKey(code)) {
			log.warn("Fund does not hold " + code);
			return;
		}

		FundHolding updatedHolding = decreaseHolding(holdings.get(code), qty, gross);
		if (updatedHolding.getQuantity().compareTo(BigDecimal.ZERO) <= 0 && updatedHolding.getLatestTradeTime() <= 0)
			holdings.remove(code);
		else
			holdings.put(code, updatedHolding);
	}

	public FundHolding payInterest(String code, BigDecimal interest) {
		FundHolding holding = holdings.get(code);
		if (holding != null) {
			holding = FundHolding.create(code, holding.getQuantity(), holding.getGross().subtract(interest));
			holdings.put(code, holding);
		}
		return holding;
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

	public Date getDate() { return this.date; }
	public Map<String, FundHolding> getHoldings() { return holdings; }

	private FundHolding increaseHolding(FundHolding fundHolding, BigDecimal qty, BigDecimal gross) {
		return FundHolding.create(fundHolding.getCode(), fundHolding.getQuantity().add(qty), gross.add(fundHolding.getGross()))
				.setLatestTradeTime(fundHolding.getLatestTradeTime());
	}

	private FundHolding decreaseHolding(FundHolding fundHolding, BigDecimal qty,
										BigDecimal gross) {
		BigDecimal orgGross = fundHolding.getPrice().multiply(qty);
		return FundHolding.create(fundHolding.getCode(),
						fundHolding.getQuantity().subtract(qty),
						fundHolding.getGross().subtract(orgGross))
				.setLatestTradeTime(fundHolding.getLatestTradeTime());
	}

	public Fund calculateNetProfit(Map<String, StockQuote> quoteMap) {
		holdings
				.entrySet().stream()
				.filter(entry -> quoteMap.containsKey(entry.getKey()) && quoteMap.get(entry.getKey()).hasPrice())
				.forEach(entry -> entry.getValue().calculateNetProfit(new BigDecimal(quoteMap.get(entry.getKey()).getPrice())));

		netProfit = holdings.values().stream()
				.map(v -> v.netProfit())
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		return this;
	}

	public BigDecimal netProfit() {
		return netProfit;
	}

	@Transient
	public BigDecimal gross() {
		return holdings.values().stream()
				.map(v -> v.getGross())
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public void setProfit(BigDecimal profit) {
		this.profit = profit;
	}
	public BigDecimal getProfit() {
		return profit;
	}

	public BigDecimal getNetProfit() {return netProfit;}
	public void setNetProfit(BigDecimal netProfit) {this.netProfit = netProfit;}

	public BigDecimal getCashoutAmount() { return cashoutAmount; }
	public Fund setCashoutAmount(BigDecimal cashoutAmount) {
		this.cashoutAmount = cashoutAmount;
		return this;
	}

	public BigDecimal getCashinAmount() { return cashinAmount;}
	public Fund setCashinAmount(BigDecimal cashinAmount) {
		this.cashinAmount = cashinAmount;
		return this;
	}

	public FundType getType() { return type; }
	public Fund setType(FundType type) {
		this.type = type;
		return this;
	}

	public boolean containSymbol(String symbol) {
		return holdings.containsKey(symbol);
	}

}
