package squote.web.parser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.common.base.Optional;

public class TvboxnowThreadRetriever extends ForumThreadParser {
	
	public TvboxnowThreadRetriever(String url, String source) {
		super(url, source, "UTF-8", Optional.of("http://www.tvboxnow.com/logging.php?action=login&loginsubmit=yes&username=khordad&password=khordad"));
	}

	@Override
	protected String parseURL(Element e) {
		return e.select("span[id^=thread_] a[href^=thread-]").attr("href");
	}

	@Override
	protected String parseTitle(Element e) {
		return e.select("span[id^=thread] a[href^=thread-]").text();
	}

	@Override
	protected String parseDateStr(Element e) {
		return e.select("td.author em").text();
	}

	@Override
	protected Elements parseThreads(Document doc) {
		return doc.select("tbody[id*=thread_]");
	}
	
}

