package squote.domain;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.data.annotation.Id;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MarketDailyReport {
	@Id
	private int date;

	private StockQuote hsi;
	private StockQuote hscei;
	
	public static MarketDailyReport EMPTY_REPORT = new MarketDailyReport();
	
	public MarketDailyReport() {}

	public MarketDailyReport(Date date) { this.date = formatDate(date); }

	public MarketDailyReport(int yyyymmdd, StockQuote hsi, StockQuote hscei) {
		this.date = yyyymmdd;
		this.hsi = hsi;
		this.hscei = hscei;
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
