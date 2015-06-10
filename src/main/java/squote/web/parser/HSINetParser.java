package squote.web.parser;
 
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import squote.SquoteConstants;
import squote.domain.StockQuote;
import thc.util.DateUtils;
import thc.util.HttpClientImpl;
import thc.util.NumberUtils;

public class HSINetParser extends WebParser<StockQuote> {	
	private static Logger log = LoggerFactory.getLogger(HSINetParser.class);
	
	static String DailyReportURL = "http://www.hsi.com.hk/HSI-Net/static/revamp/contents/en/indexes/report/{0}/idx_{1}.csv";
	
	private final SquoteConstants.IndexCode index;
	private final Date date;
	
	public static SquoteConstants.IndexCode Index(SquoteConstants.IndexCode i) { return i; }
	public static Date Date(Date d) { return d; }
	public static Date Date(String yyyyMMdd) throws ParseException {
		return new SimpleDateFormat("yyyyMMdd").parse(yyyyMMdd);
	}
	
	public HSINetParser(SquoteConstants.IndexCode index, Date date) {
		this.index = index;
		this.date = date;
	}
	
	public Optional<StockQuote> parse() {		
		if (DateUtils.isOverMonth(date, new Date(), 2)) {
			return Optional.empty();
		}
		
		SimpleDateFormat format = new SimpleDateFormat("dMMMyy", Locale.US);
		String url = MessageFormat.format(DailyReportURL, StringUtils.lowerCase(index.toString().toLowerCase()), format.format(date));
		try {
			List<String> csv = IOUtils.readLines(new HttpClientImpl("utf-8").newInstance().makeGetRequest(url));
			String[] result = csv.get(4).split("\t");
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
			return Optional.empty();
		}
	}
}