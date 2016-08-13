package squote.domain;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.data.annotation.Id;

import javax.validation.constraints.Min;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MarketDailyReport {
	@Id
	@Min(20120000)
	private int date;
		
	private MonetaryBase moneyBase;
	private StockQuote hsi;
	private StockQuote hscei;
	
	public static MarketDailyReport EMPTY_REPORT = new MarketDailyReport();
	
	public MarketDailyReport() {}

	public MarketDailyReport(Date date) { this.date = formatDate(date); }
	public MarketDailyReport(Date date, MonetaryBase moneyBase, StockQuote hsiQuote, StockQuote hsceQuote) {
		this(date);
		this.moneyBase = moneyBase;
		this.hsi = hsiQuote;
		this.hscei = hsceQuote;
	}
	public MarketDailyReport(String yyyymmdd, MonetaryBase moneyBase, StockQuote hsiQuote, StockQuote hsceQuote) {
		this.date = Integer.valueOf(yyyymmdd);
		this.moneyBase = moneyBase;
		this.hsi = hsiQuote;
		this.hscei = hsceQuote;
	}

	public static int formatDate(Date date) { return Integer.valueOf(new SimpleDateFormat("yyyyMMdd").format(date)); }

	@Override
	public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
	
	public boolean isEmpty() {
		return date < 20120000;
	}

	public int getDate() {
        return this.date;
    }

	public void setDate(int date) {
        this.date = date;
    }

	public MonetaryBase getMoneyBase() {
        return this.moneyBase;
    }

	public void setMoneyBase(MonetaryBase moneyBase) {
        this.moneyBase = moneyBase;
    }

	public StockQuote getHsi() {
        return this.hsi;
    }

	public void setHsi(StockQuote hsi) {
        this.hsi = hsi;
    }

	public StockQuote getHscei() {
        return this.hscei;
    }

	public void setHscei(StockQuote hscei) {
        this.hscei = hscei;
    }

}
