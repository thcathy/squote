package squote.web.parser;

import java.util.Optional;

import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import squote.domain.StockQuote;
import thc.util.HttpClientImpl;
import thc.util.StringUtils;

public class EtnetStockQuoteParser {
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	private static final String URL = "http://www.etnet.com.hk/www/tc/stocks/realtime/quote.php?code=";
			
	public Optional<StockQuote> parse(String code) {		
		try {
			Document doc = new HttpClientImpl("UTF-8").newInstance().getDocument(URL + code);
						
			StockQuote q = new StockQuote(code);
			q.setPrice(doc.select("div[id^=StkDetailMainBox] span[class^=Price ]").text().replaceAll("[0\\D]+$",""));
			
			String[] changes = doc.select("div[id^=StkDetailMainBox] span[class^=Change]").text().split(" ");
			q.setChangeAmount(changes[0]);
			q.setChange(changes[1].replace("(", "").replace(")",""));

			q.setHigh(doc.select("div[id^=StkDetailMainBox] tr:eq(0) td:eq(1) span.Number").text());
			q.setLow(doc.select("div[id^=StkDetailMainBox] tr:eq(1) td:eq(0) span.Number").text());

			Optional<String> updateTime = StringUtils.extractText(doc.select("div[id^=StkDetailTime]").text(), "[0-9]*/[0-9]*/[0-9]* [0-9]*:[0-9]*");
			updateTime.ifPresent(s -> q.setLastUpdate(s));
			
			q.setPe(doc.select("div[id^=StkList] li:eq(33)").text().split("/")[0].trim());
			q.setYield(doc.select("div[id^=StkList] li:eq(37)").text().split("/")[0].trim() + "%");
			q.setNAV(doc.select("div[id^=StkList] li:eq(49)").text());
			q.setYearHigh(doc.select("div[id^=StkList] li:eq(19)").text());
			q.setYearLow(doc.select("div[id^=StkList] li:eq(23)").text());
			
			log.debug("parsed quote: {}", q);
			
			return Optional.of(q);
		} catch (Exception e) {
			log.warn("Cannot get quote from Etnet for code :" + code, e);
			return Optional.empty();
		}	
	}	



//	assertTrue(NumberUtils.isNumber(q.getYearLow()));
//	assertTrue(NumberUtils.isNumber(q.getYearHigh()));

}
