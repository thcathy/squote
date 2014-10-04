package squote.web.parser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import thc.util.HttpClient;

public class IndexConstituentParser {
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	public static String HSIConstituentsURL = "http://www.etnet.com.hk/www/tc/stocks/indexes_detail.php?subtype=HSI";
	public static String HCEIConstituentsURL = "http://www.etnet.com.hk/www/tc/stocks/indexes_detail.php?subtype=cei";
	public static String HCCIConstituentsURL= "http://www.etnet.com.hk/www/tc/stocks/indexes_detail.php?subtype=cci";
	public static String MSCIChinaConstituentsURL = "http://hk.ishares.com/misc/fund_holdings_eq.htm?ticker=2801&assetClass=EQ&periodCd=d&startPosition=0&sortField=&sortDirection=ASC&viewRows=&filterField=&filterValue=&isAjax=true";

	public List<String> getHSIConstituents() {
		return getEtnetIndexConstituents(HSIConstituentsURL);
	}

	public List<String> getHCEIConstituents() {
		return getEtnetIndexConstituents(HCEIConstituentsURL);
	}

	public List<String> getHCCIConstituents() {
		return getEtnetIndexConstituents(HCCIConstituentsURL);
	}

	public List<String> getEtnetIndexConstituents(String url) {
		List<String> results = new ArrayList<String>();
		try
		{			
			Document doc =  new HttpClient("UTF-8").getDocument(url);
			for (Iterator<Element> i = doc.select("a[href^=realtime/quote.php?code=]").iterator(); i.hasNext();) {				
				Element e = i.next();
				if (StringUtils.isNumeric(e.html())) results.add(Integer.valueOf(e.html()).toString());
			}
		} catch (Exception e) {
			log.error("Fail to retrieve list for url: " + url,e);
		}
		return results;
	}

	public List<String> getMSCIChinaConstituents() {		
		List<String> results = new ArrayList<String>();		
		try
		{
			Document doc = Jsoup.parseBodyFragment("<table>" + IOUtils.toString(new HttpClient().makeGetRequest(MSCIChinaConstituentsURL)) + "</table>");
			for (Iterator<Element> i = doc.select("td.cell").iterator(); i.hasNext();) {				
				Element e = i.next();
				if ("XHKG".equals(e.html())) results.add(e.previousElementSibling().html());
			}
		} catch (Exception e) {
			log.error("Fail to retrieve list of MSCI China Constituents",e);
		}
		return results;
	}

	

}

