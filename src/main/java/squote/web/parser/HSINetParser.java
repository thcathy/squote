package squote.web.parser;
 
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import squote.domain.StockQuote;
import thc.util.HttpClient;
import thc.util.NumberUtils;

import com.google.common.base.Optional;
 
public class HSINetParser extends WebParser<StockQuote> {	
	private static Logger log = LoggerFactory.getLogger(HSINetParser.class);
	
	public enum IndexCode {HSI, HSCEI}
		
	static String DailyReportURL = "http://www.hsi.com.hk/HSI-Net/static/revamp/contents/en/indexes/report/{0}/idx_{1}.csv";
	
	private final IndexCode index;
	private final Date date;
	
	public static IndexCode Index(IndexCode i) { return i; }
	public static Date Date(Date d) { return d; }
	public static Date Date(String yyyyMMdd) throws ParseException {
		return new SimpleDateFormat("yyyyMMdd").parse(yyyyMMdd);
	}
	
	public HSINetParser(IndexCode index, Date date) {
		this.index = index;
		this.date = date;
	}
	
	public Optional<StockQuote> parse() {
		SimpleDateFormat format = new SimpleDateFormat("dMMMyy", Locale.US);
		String url = MessageFormat.format(DailyReportURL, StringUtils.lowerCase(index.toString()), format.format(date));
		try {
			String[] result = IOUtils.readLines(new HttpClient("utf-8").makeGetRequest(url)).get(4).split("\t");
			StockQuote quote = new StockQuote(index.toString());
			quote.setLastUpdate(NumberUtils.extractNumber(result[0]));
			quote.setHigh(NumberUtils.extractNumber(result[3]));
			quote.setLow(NumberUtils.extractNumber(result[4]));
			quote.setPrice(NumberUtils.extractNumber(result[5]));
			quote.setChangeAmount(NumberUtils.extractNumber(result[6]));
			quote.setChange(NumberUtils.extractNumber(result[7]));
			quote.setYield(NumberUtils.extractNumber(result[8]));
			quote.setPe(NumberUtils.extractNumber(result[9]));
			return Optional.of(quote);
		} catch (Exception e) {
			log.warn("Fail to retrieveDailyReportFromHSINet: reason: {} url: {}", e.getMessage(),url);
			return Optional.absent();
		}
	}
}