package squote.web.parser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.common.base.Optional;

public class UwantsThreadRetriever extends ForumThreadParser {

	public UwantsThreadRetriever(String url, String source) {
		super(url, source, "Big5", Optional.<String>absent());
	}

	@Override
	protected String parseURL(Element e) {
		return e.select("span[id^=thread] a[href^=viewthread.php]").attr("href");
	}

	@Override
	protected String parseTitle(Element e) {
		return e.select("span[id^=thread] a[href^=viewthread.php]").text();
	}

	@Override
	protected String parseDateStr(Element e) {
		return e.select("td.author em").text();
	}

	@Override
	protected Elements parseThreads(Document doc) {
		return doc.select("tbody[id^=normal]");
	}	
}
