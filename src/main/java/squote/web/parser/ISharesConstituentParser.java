package squote.web.parser;
 
import static org.apache.commons.lang3.CharEncoding.UTF_8;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import thc.util.HttpClientImpl;
 
public class ISharesConstituentParser {
	private static final Logger log = LoggerFactory.getLogger(ISharesConstituentParser.class);
	
	public static String MSCIChinaConstituentsURL = "https://www.blackrock.com/hk/en/terms-and-conditions?targetUrl=%2Fhk%2Fen%2F251576%2Ffund-download.dl&action=ACCEPT";
		
	public static List<String> parseMSCIChina() {		
		List<String> stockCodes = new ArrayList<>();
		try {
			InputStream out = new HttpClientImpl(UTF_8).newInstance().makeGetRequest(MSCIChinaConstituentsURL);
			Document doc = Jsoup.parse(out, UTF_8, MSCIChinaConstituentsURL);
			Elements es = doc.getElementsMatchingOwnText("XHKG");
			for (Element e : es) stockCodes.add(e.parent().siblingElements().first().child(0).ownText());
		} catch (Exception e) {
			log.error("Fail to retrieve list of MSCI China Constituents",e);
		}		
		
		log.debug("Parsed MSCI China Stock Codes {}", stockCodes);
		return stockCodes;
	}
  	
	public static List<String> parseMSCIHK() {
		String URL = "http://www.ishares.com/us/products/239657/ishares-msci-hong-kong-etf/1395165510754.ajax?fileType=csv&fileName=EWH_holdings&dataType=fund";
		
		try {
			List<String> lines = IOUtils.readLines(new HttpClientImpl(UTF_8).newInstance().makeGetRequest(URL));
			List<String> results = lines.stream()				
				.map(line->line.split("\",\""))				
				.filter(line -> line.length > 12 && line[11].contains("Hong Kong Exchanges And Clearing Ltd"))
				.map(cs->cs[0].replaceAll("\"", ""))
				.collect(Collectors.toList());
			return results;
		} catch (Exception e) {
			log.error("Fail to retrieve list of MSCI HK Constituents",e);
			return Collections.emptyList();
		}	
	}
}