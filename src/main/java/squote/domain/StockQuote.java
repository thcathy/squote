package squote.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import thc.util.NumberUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import static squote.SquoteConstants.NA;

public class StockQuote {
	private String stockCode = "";
	private String stockName = "";
	private String lastUpdate = NA;
	private String price = NA;
	private String high = NA;
	private String low = NA;
	private String change = NA;
	private String changeAmount = NA;
	private String pe = NA;
	private String yield = NA;
	private String NAV = NA;
	private String yearLow = NA;
	private String yearHigh = NA;
	private Double yearHighPercentage = null;
	private Double lastYearPercentage = null;
	private Double last2YearPercentage = null;
	private Double last3YearPercentage = null;

	private Map<Integer, Double> previousPriceMap = new HashMap<Integer, Double>();

	public StockQuote() {}

	public StockQuote(String code) {
		this.stockCode = code.replaceFirst("^0+(?!$)", "");
	}
	
	public StockQuote setPrice(String price) {
		yearHighPercentage = null;
		this.price = price;
		return this;
	}
	
	public void setYearHigh(String yearHigh) {
		yearHighPercentage = null;
		this.yearHigh = yearHigh;
	}

	public double getYearHighPercentage() {
		try {
			if (yearHighPercentage == null) {
				double yearHighValue = Double.parseDouble(yearHigh);
				double realPrice = Double.parseDouble(price);
				yearHighPercentage = BigDecimal.valueOf(((realPrice - yearHighValue) / yearHighValue) * 100).setScale(2,RoundingMode.HALF_UP).doubleValue();
			}
		} catch (Exception e) {
			yearHighPercentage = 0.0;
		}
		return yearHighPercentage;
	}

	public void setPreviousPrice(int previousYear, double price) {
		previousPriceMap.put(previousYear, price);
	}

	public Double getPreviousPrice(int previousYear) {
		return previousPriceMap.get(previousYear);
	}

	public double getPreviousYearPercentage(int previousYear) {
		double percentage = 0;
		try {
			double previousPrice = getPreviousPrice(previousYear);
			double realPrice = Double.parseDouble(price);
			percentage = BigDecimal.valueOf(((realPrice - previousPrice) / previousPrice) * 100).setScale(2,RoundingMode.HALF_UP).doubleValue();
		} catch (Exception e) {
			percentage = 0.0;
		}
		return percentage;
	}

	public Double getLastYearPercentage() {
		if (lastYearPercentage == null) {
			lastYearPercentage = getPreviousYearPercentage(1);
		}
		return lastYearPercentage;
	}


	public Double getLast2YearPercentage() {
		if (last2YearPercentage == null) {
			last2YearPercentage = getPreviousYearPercentage(2);
		}
		return last2YearPercentage;
	}


	public Double getLast3YearPercentage() {
		if (last3YearPercentage == null) {
			last3YearPercentage = getPreviousYearPercentage(3);
		}
		return last3YearPercentage;
	}

	@JsonIgnore
	public Double getPriceDoubleValue() { return NumberUtils.extractDouble(price); }

	@Override
	public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

	public String getStockCode() {
        return this.stockCode;
    }

	public void setStockCode(String stockCode) {
        this.stockCode = stockCode.replaceFirst("^0+(?!$)", "");
    }

	public String getLastUpdate() {
        return this.lastUpdate;
    }

	public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

	public String getPrice() {
        return this.price;
    }

    public boolean hasPrice() { return StringUtils.isNotBlank(price) && !"NA".equals(price); }

	public String getHigh() {
        return this.high;
    }

	public void setHigh(String high) {
        this.high = high;
    }

	public String getLow() {
        return this.low;
    }

	public void setLow(String low) {
        this.low = low;
    }

	public String getChange() {
        return this.change;
    }

	public void setChange(String change) {
        this.change = change;
    }

	public String getChangeAmount() {
        return this.changeAmount;
    }

	public void setChangeAmount(String changeAmount) {
        this.changeAmount = changeAmount;
    }

	public String getPe() {
        return this.pe;
    }

	public void setPe(String pe) {
        this.pe = pe;
    }

	public String getYield() {
        return this.yield;
    }

	public void setYield(String yield) {
        this.yield = yield;
    }

	public String getNAV() {
        return this.NAV;
    }

	public void setNAV(String NAV) {
        this.NAV = NAV;
    }

	public String getYearLow() {
        return this.yearLow;
    }

	public void setYearLow(String yearLow) {
        this.yearLow = yearLow;
    }

	public String getYearHigh() {
        return this.yearHigh;
    }

	public void setYearHighPercentage(Double yearHighPercentage) {
        this.yearHighPercentage = yearHighPercentage;
    }

	public void setLastYearPercentage(Double lastYearPercentage) {
        this.lastYearPercentage = lastYearPercentage;
    }

	public void setLast2YearPercentage(Double last2YearPercentage) {
        this.last2YearPercentage = last2YearPercentage;
    }

	public void setLast3YearPercentage(Double last3YearPercentage) {
        this.last3YearPercentage = last3YearPercentage;
    }

	public Map<Integer, Double> getPreviousPriceMap() {
        return this.previousPriceMap;
    }

	public StockQuote setPreviousPriceMap(Map<Integer, Double> previousPriceMap) {
		this.previousPriceMap = previousPriceMap;
		return this;
	}

	public String getStockName() {
		return stockName;
	}

	public void setStockName(String stockName) {
		this.stockName = stockName;
	}
}
