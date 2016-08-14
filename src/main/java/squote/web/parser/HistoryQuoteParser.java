package squote.web.parser;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Optional;

import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import thc.util.HttpClientImpl;
import thc.util.NumberUtils;


public class HistoryQuoteParser {
	private static Logger log = LoggerFactory.getLogger(HistoryQuoteParser.class);
	
	private static String YAHOO_HISTORY_QUOTE_URL = "http://hk.finance.yahoo.com/q/hp?s={0}&a={2,number,##}&b={1,date,dd}&c={1,date,yyyy}&d={4,number,##}&e={3,date,dd}&f={3,date,yyyy}&g=d";

	public Optional<BigDecimal> getPreviousYearQuote(String stockNumber, int previousYear) {
		Calendar fromDate = Calendar.getInstance();
		Calendar toDate = Calendar.getInstance();
		
		fromDate.add(Calendar.YEAR, 0-previousYear);		
		toDate.add(Calendar.YEAR, 0-previousYear);
		toDate.add(Calendar.DATE, 7);

		while (stockNumber.length() < 4) {
			stockNumber = "0" + stockNumber;
		}
		stockNumber += ".HK";

		return getQuoteAtDate(stockNumber, fromDate, toDate);
	}
	
	public Optional<BigDecimal> getQuoteAtDate(String stock, Calendar fromDate, Calendar toDate) {		
		String url = getQuoteURL(stock,fromDate,toDate);
		try
		{	
			Document doc = new HttpClientImpl("utf-8").newInstance().getDocument(url);
			String price = doc.select("td[class^=yfnc_tabledata1]:eq(6)").first().text();
			return Optional.of( new BigDecimal(NumberUtils.extractDouble(price)));
		} catch (Exception e) {			
			log.warn("Fail to getHistoryPrice quote at date from url: {}, Reason {}", url, e);
			return Optional.empty();
		}
	}
	
	private String getQuoteURL(String stock, Calendar fromDate, Calendar toDate) {
		return MessageFormat.format(YAHOO_HISTORY_QUOTE_URL, stock, fromDate.getTime(), fromDate.get(Calendar.MONTH), toDate.getTime(), toDate.get(Calendar.MONTH));
	}	
}

