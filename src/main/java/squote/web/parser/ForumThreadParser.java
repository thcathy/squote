package squote.web.parser;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import squote.domain.ForumThread;
import thc.util.HttpClient;

import com.google.common.base.Optional;

public abstract class ForumThreadParser implements Callable<List<ForumThread>> {
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	protected final SimpleDateFormat simpleDFormat = new SimpleDateFormat("yyyy-M-d");
	
	protected final String url;
	protected final String source;	
	protected final String encoding;
	protected final Optional<String> loginUrl;	
			
	// Constructor
	protected ForumThreadParser(String url, String source, String encoding, Optional<String> loginUrl) {
		this.url = url;
		this.source = source;
		this.loginUrl = loginUrl;
		this.encoding = encoding;
	}
	
	/**
	 * Factory method to build a parser instance
	 */
	public static ForumThreadParser buildParser(String url, int page) {
		if (url.contains("www.uwants.com")) 
			return new UwantsThreadRetriever(String.format(url,page), "Uwants");
		else if (url.contains("www.discuss.com"))
			return new UwantsThreadRetriever(String.format(url,page), "Discuss");
		else if (url.contains("www.tvboxnow.com"))
			return new TvboxnowThreadRetriever(String.format(url,page), "Tvboxnow");			
		else
			throw new IllegalArgumentException("No parser for url: " + url);				
	}
		
	@Override
	public List<ForumThread> call() throws Exception {
		return parse();
	}
	
	public List<ForumThread> parse() {
		log.debug("parse url: {}", url);
		HttpClient httpClient = new HttpClient(encoding);		
		List<ForumThread> results = new ArrayList<ForumThread>();
		
		if (loginUrl.isPresent()) httpClient.makeGetRequest(loginUrl.get());	// perform login for particular forum
		
		try {
			for (Iterator<Element> iter = parseThreads(httpClient.getDocument(url)).iterator(); iter.hasNext(); ) {				
				Element e = iter.next();
				results.add(new ForumThread(new URL(url).getHost() + "/" + parseURL(e), parseTitle(e), source, convertDate(parseDateStr(e))));
			}
		} catch (Exception e1) {
			log.warn("Fail to parse url:" + url, e1);
		}		
		
		return results;
	}
		
	protected Date convertDate(String str) {		
		try {
			return simpleDFormat.parse(str);
		} catch (ParseException e) {
			log.warn("Cannot parse date: ", str);
			return new Date();
		}
	}
	
	abstract protected String parseURL(Element e);
	abstract protected String parseTitle(Element e);
	abstract protected String parseDateStr(Element e);
	abstract protected Elements parseThreads(Document doc);	
}
