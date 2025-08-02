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
import java.util.stream.Collectors;

public class Fund {
	private static final Logger log = LoggerFactory.getLogger(Fund.class);
	private static final String DOT_REPLACEMENT = "___DOT___";

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
	private Map<String, AlgoConfig> algoConfigs = new ConcurrentHashMap<>();
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

	private String encodeKey(String key) {
		return key.replace(".", DOT_REPLACEMENT);
	}

	private String decodeKey(String encodedKey) {
		return encodedKey.replace(DOT_REPLACEMENT, ".");
	}

	private boolean isHoldingsContainsKey(String code) {
		return holdings.containsKey(encodeKey(code));
	}

	private FundHolding getHolding(String code) {
		return holdings.get(encodeKey(code));
	}

	private void putHolding(String code, FundHolding holding) {
		holdings.put(encodeKey(code), holding);
	}

	private void removeHolding(String code) {
		holdings.remove(encodeKey(code));
	}

	public void removeStock(String code) {
		removeHolding(code);
	}

	public void putAlgoConfig(String code, AlgoConfig algoConfig) {
		algoConfigs.put(encodeKey(code), algoConfig);
	}

	public void removeAlgoConfig(String code) {
		algoConfigs.remove(encodeKey(code));
	}

	public void buyStock(String code, BigDecimal qty, BigDecimal gross) {
		if (isHoldingsContainsKey(code)) {
			putHolding(code, increaseHolding(getHolding(code), qty, gross));
		} else {
			putHolding(code, FundHolding.create(code, qty, gross));
		}
	}

	public void sellStock(String code, BigDecimal qty, BigDecimal gross) {
		if (!isHoldingsContainsKey(code)) {
			log.warn("Fund does not hold " + code);
			return;
		}

		FundHolding updatedHolding = decreaseHolding(getHolding(code), qty, gross);
		if (updatedHolding.getQuantity().compareTo(BigDecimal.ZERO) <= 0 && updatedHolding.getLatestTradeTime() <= 0)
			removeHolding(code);
		else
			putHolding(code, updatedHolding);
	}

	public FundHolding payInterest(String code, BigDecimal interest) {
		FundHolding holding = getHolding(code);
		if (holding != null) {
			holding = FundHolding.create(code, holding.getQuantity(), holding.getGross().subtract(interest));
			putHolding(code, holding);
		}
		return holding;
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

	public Date getDate() { return this.date; }
	public Map<String, FundHolding> getHoldings() {
		return holdings.entrySet().stream()
			.collect(Collectors.toMap(
				entry -> decodeKey(entry.getKey()),
				Map.Entry::getValue,
				(existing, replacement) -> replacement,
				ConcurrentHashMap::new
			));
	}

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
				.map(FundHolding::netProfit)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		return this;
	}

	public BigDecimal netProfit() {
		return netProfit;
	}

	@Transient
	public BigDecimal gross() {
		return holdings.values().stream()
				.map(FundHolding::getGross)
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
		return isHoldingsContainsKey(symbol);
	}

	public Map<String, AlgoConfig> getAlgoConfigs() {
		return algoConfigs.entrySet().stream()
			.collect(Collectors.toMap(
				entry -> decodeKey(entry.getKey()),
				Map.Entry::getValue,
				(existing, replacement) -> replacement,
				ConcurrentHashMap::new
			));
	}

//	public void setAlgoConfigs(Map<String, AlgoConfig> algoConfigs) {
//		this.algoConfigs = algoConfigs.entrySet().stream()
//			.collect(Collectors.toMap(
//				entry -> encodeKey(entry.getKey()),
//				Map.Entry::getValue,
//				(existing, replacement) -> replacement,
//				ConcurrentHashMap::new
//			));
//	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

//	public void setHoldings(Map<String, FundHolding> holdings) {
//		this.holdings = holdings.entrySet().stream()
//			.collect(Collectors.toMap(
//				entry -> encodeKey(entry.getKey()),
//				Map.Entry::getValue,
//				(existing, replacement) -> replacement,
//				ConcurrentHashMap::new
//			));
//	}

	public void setDate(Date date) {
		this.date = date;
	}
}
