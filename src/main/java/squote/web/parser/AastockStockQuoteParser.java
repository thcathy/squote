package squote.web.parser;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
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
			client.setCookie("AALTP", "1", "/", "www.aastocks.com");
			Document doc = client.getDocument(AASTOCK_STOCK_QUOTE_URL + StringUtils.leftPad(code, 5, '0'));
			// price
			quote.setPrice(doc.select("ul.UL1 li.LI1:containsOwn(Last)").first().parent().nextElementSibling().child(0).child(0).html());
			
			// stock name
			String name = doc.select("div#quotediv table table div font b").text();
			quote.setStockName(name.substring(0, name.length()-9));
			
			//*[@id="quotediv"]/table[1]/tbody/tr[1]/td/table/tbody/tr/td[1]/div[1]/font[1]/b
			
			// change
			Elements divs = doc.select("ul.UL1.W1 li.LI1:contains(Chg)");
			boolean isDown = divs.get(0).parent().nextElementSibling().select("img").first().attr("src").contains("downarrow");
			quote.setChangeAmount(isDown?"-" + divs.get(0).parent().nextElementSibling().select("span.bold").first().html():"+" + divs.get(0).parent().nextElementSibling().select("span.bold").first().html());
			quote.setChange(isDown?"-" + divs.get(1).parent().nextElementSibling().select("span.bold").first().html():"+" + divs.get(1).parent().nextElementSibling().select("span.bold").first().html());

			// day high day low
			quote.setHigh(doc.select("ul.UL1.W2 li.LI1:containsOwn(High)").get(0).parent().nextElementSibling().child(0).html());
			quote.setLow(doc.select("ul.UL1.W2 li.LI1:containsOwn(Low)").get(0).parent().nextElementSibling().child(0).html());

			// PE
			quote.setPe(doc.select("td.font12_grey:containsOwn(P/E Ratio)").get(0).nextElementSibling().html());
			// yield
			quote.setYield(doc.select("td.font12_grey:containsOwn(Yield)").get(0).nextElementSibling().html());
			// NAV
			quote.setNAV(doc.select("td.font12_grey:containsOwn(NAV)").get(0).nextElementSibling().html());

			// 52 high low
			String[] yearHighLow = doc.select("ul.UL2 li.LI3:containsOwn(52 Week Range)").get(0).parent().nextElementSibling().child(0).html().split(" - ");
			quote.setYearLow(yearHighLow[0]);
			quote.setYearHigh(yearHighLow[1]);

			// last update
			quote.setLastUpdate(doc.select("font.font12_white:containsOwn(Last Update:)").get(0).nextElementSibling().html());
		} catch (Exception e) {
			log.error("Cannot parse stock code: {}", code);
		}
		return quote;
	}

}
