package squote.web.parser;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import squote.domain.StockQuote;
import thc.util.HttpClient;
import thc.util.HttpClientImpl;

public class QuamnetStockQuoteParser implements StockQuoteParser {
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	public static String QUAMNET_STOCK_QUOTE_URL = "http://www.quamnet.com/quote.action?request_locale=en_US&quoteSectionCode=&stockCode=";

	private final String code;
	
	public QuamnetStockQuoteParser(String code) { this.code = code; }
	
	@Override
	public StockQuote getStockQuote() {
		StockQuote quote = new StockQuote(code);
		try
		{			
			HttpClient client = new HttpClientImpl("UTF-8").newInstance();			
			Document doc = client.getDocument(QUAMNET_STOCK_QUOTE_URL + code);
			Element elementLastPrice = doc.select("div:containsOwn(Last Price)").first();
			
			// price
			quote.setPrice(elementLastPrice.nextElementSibling().html());
			
			// stock name
			String name = doc.select("a:containsOwn(Full company)").first().parent().previousElementSibling().previousElementSibling().html();
			quote.setStockName(name.replaceAll("&nbsp.+", ""));
			
			// change			
			boolean isDown = elementLastPrice.nextElementSibling().nextElementSibling().select("img").first().attr("src").contains("down");
			quote.setChangeAmount(isDown?
					"-" + elementLastPrice.nextElementSibling().nextElementSibling().text().replaceAll(" \\(.+\\)","").substring(1)
					: "+" + elementLastPrice.nextElementSibling().nextElementSibling().text().replaceAll(" \\(.+\\)","").substring(1));
			quote.setChange(isDown?"-" + elementLastPrice.nextElementSibling().nextElementSibling().text().replaceAll("^(.+?)\\(","").replace(")","")
					:"+" + elementLastPrice.nextElementSibling().nextElementSibling().text().replaceAll("^(.+?)\\(","").replace(")",""));

			// day high day low
			quote.setHigh(doc.select("div:containsOwn(Day High)").first().nextElementSibling().text());
			quote.setLow(doc.select("div:containsOwn(Day Low)").first().nextElementSibling().text());

			// PE
			quote.setPe(doc.select("span:containsOwn(P/E)").first().parent().nextElementSibling().text());
			// yield
			quote.setYield(doc.select("span:containsOwn(Yield)").first().parent().nextElementSibling().text());
			// NAV
			quote.setNAV(doc.select("span:containsOwn(NAV)").first().parent().nextElementSibling().text());

			// 52 high low			
			quote.setYearLow(doc.select("div:containsOwn(52Wk Low)").first().nextElementSibling().text());
			quote.setYearHigh(doc.select("div:containsOwn(52Wk High)").first().nextElementSibling().text());

			// last update
			quote.setLastUpdate(doc.select("td:containsOwn(As of)").first().text().substring(6));
		} catch (Exception e) {
			log.error("Cannot parse stock code: {}", code);
		}
		return quote;
	}

}
