package squote.web.parser;

import java.text.MessageFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import squote.domain.MonetaryBase;
import thc.util.HttpClient;
import thc.util.NumberUtils;

public class HKMAMonetaryBaseParser implements Callable<java.util.Optional<MonetaryBase>> {
	private static Logger log = LoggerFactory.getLogger(HKMAMonetaryBaseParser.class);

	static String DailyMonetaryBaseURL= "http://www.hkma.gov.hk/eng/market-data-and-statistics/monetary-statistics/monetary-base/{0,date,yyyy}/{0,date,yyyyMMdd}-1.shtml";
	
	private final Date date;
	
	public HKMAMonetaryBaseParser(Date date) { this.date = date; }
		
	private static double getNumber(Document doc, String title) { 
		for (Iterator<Element> i = doc.select("td.heading").iterator(); i.hasNext();) {
			Element e = i.next();
			if (e.text().contains(title)) 
				return NumberUtils.extractDouble(e.nextElementSibling().child(0).text());
		}
		throw new RuntimeException("Cannot find number:" + title);
	}
	
	public static java.util.Optional<MonetaryBase> retrieveMonetaryBase(Date date) {
		String url = MessageFormat.format(DailyMonetaryBaseURL, date);
		try {
			Document doc = new HttpClient().getDocument(url);
			return Optional.of(
				new MonetaryBase(
					getNumber(doc, "Certificates of Indebtedness"),
					getNumber(doc,"Government Notes/Coins in Circulation"),
					getNumber(doc,"Closing Aggregate Balance"),
					getNumber(doc,"Outstanding Exchange Fund Bills and Notes"))
			);			
		} catch (Exception e) {
			log.info("Fail to retrieveDailyMonetaryBaseFromURL:" + url,e);
			return Optional.empty();
		}		
	}

	@Override
	public Optional<MonetaryBase> call() throws Exception {
		return retrieveMonetaryBase(date);
	}		
}
    

