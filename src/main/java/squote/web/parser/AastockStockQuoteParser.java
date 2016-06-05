package squote.web.parser;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import squote.domain.StockQuote;
import thc.util.HttpClient;
import thc.util.HttpClientImpl;

public class AastockStockQuoteParser implements StockQuoteParser {
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	public static String AASTOCK_STOCK_QUOTE_URL = "http://www.aastocks.com/EN/stock/DetailQuote.aspx?&symbol=";

	private final String code;
	
	public AastockStockQuoteParser(String code) { this.code = code; }
	
	@Override
	public StockQuote getStockQuote() {
		StockQuote quote = new StockQuote(code);
		try
		{			
			HttpClient client = new HttpClientImpl("UTF-8").newInstance();
			client.makeGetRequest(AASTOCK_STOCK_QUOTE_URL + StringUtils.leftPad(code, 5, '0'));
			Document doc = client.getDocument(AASTOCK_STOCK_QUOTE_URL + StringUtils.leftPad(code, 5, '0'));

			// price
			quote.setPrice(doc.select("div[id=labelLast] span").first().text().substring(1));

			// stock name
			quote.setStockName(doc.select("span[id$=StockName").first().text());

			// change
			quote.setChangeAmount(doc.select("div:containsOwn(Change").first().nextElementSibling().nextElementSibling().text());
			quote.setChange(doc.select("div:containsOwn(Change(%))").first().nextElementSibling().nextElementSibling().text());

			// day high day low
			String[] range = doc.select("div:containsOwn(Range)").first().nextElementSibling().nextElementSibling().text().split(" ");
			quote.setHigh(range[0]);
			quote.setLow(range[2]);

			// PE
			quote.setPe(parse(() -> doc.select("div[id=tbPERatio]").first().child(1).text().split(" / ")[0].substring(1)));
			// yield
			quote.setYield(parse(() -> doc.select("div:containsOwn(Yield/)").first().parent().parent().child(1).text().split(" / ")[0].substring(1)));
			// NAV
			quote.setNAV(parse(() -> doc.select("div[id=tbPBRatio]").first().child(1).text().split(" / ")[1]));

			// last update
			quote.setLastUpdate(doc.select("span:containsOwn(Updated:)").first().child(0).text());

			// 52 high low
			String[] yearHighLow = doc.select("td:containsOwn(52 Week)").first().nextElementSibling().text().split(" - ");
			quote.setYearLow(yearHighLow[0]);
			quote.setYearHigh(yearHighLow[1]);
		} catch (Exception e) {
			log.error("Cannot parse stock code: {}", code);
		}
		return quote;
	}

}
