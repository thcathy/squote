package squote.web.parser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import thc.util.HttpClientImpl;

public class EtnetIndexConstituentParser {
	private static final Logger log = LoggerFactory.getLogger(EtnetIndexConstituentParser.class);
		
	public static List<String> parse(String url) {
		List<String> results = new ArrayList<String>();
		try
		{			
			Document doc =  new HttpClientImpl("UTF-8").getDocument(url);
			for (Iterator<Element> i = doc.select("a[href^=realtime/quote.php?code=]").iterator(); i.hasNext();) {				
				Element e = i.next();
				if (StringUtils.isNumeric(e.html())) results.add(Integer.valueOf(e.html()).toString());
			}
		} catch (Exception e) {
			log.error("Fail to retrieve list for url: " + url,e);
		}
		return results;
	}
}

