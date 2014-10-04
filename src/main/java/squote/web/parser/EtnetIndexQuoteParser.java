package squote.web.parser;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import squote.domain.StockQuote;
import thc.util.HttpClient;

public class EtnetIndexQuoteParser {
	protected final Logger log = LoggerFactory.getLogger(getClass());
			
	public List<StockQuote> getGlobalIndexes() {		
		ArrayList<StockQuote> indexes = new ArrayList<StockQuote>();
		
		Document doc = new HttpClient("UTF-8").getDocument("http://www.etnet.com.hk/www/eng/stocks/indexes_main.php");
		indexes.add(retrieveIndexQuote(doc.select("a[href=indexes_detail.php?subtype=HSI]").first()));
		indexes.add(retrieveIndexQuote(doc.select("a[href=indexes_detail.php?subtype=CEI]").first()));			
						
		return indexes;
	}
	
	private StockQuote retrieveIndexQuote(Element e) {
		Elements nodes = e.parent().parent().children();
		StockQuote quote = new StockQuote(e.html());
		quote.setPrice(nodes.get(1).text());
		quote.setChange(nodes.get(3).text());
		quote.setLow(nodes.get(7).text());
		quote.setHigh(nodes.get(6).text());
		return quote;
	}	

}
