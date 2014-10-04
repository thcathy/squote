package squote.web.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import squote.domain.ForumThread;

public class ForumThreadParserTest {
	private static Logger log = LoggerFactory.getLogger(ForumThreadParserTest.class);
	
	@Test
	public void parserShouldGetSthFromURL() {
		// Test Uwants
		String uwantsSource = "Uwants";
		ForumThreadParser uwants = new UwantsThreadRetriever("http://www.uwants.com/forumdisplay.php?fid=472&page=1", uwantsSource);
		List<ForumThread> result = uwants.parse();		
		checkForumThreadList(uwantsSource, "www.uwants.com", result);
		
		// Test Discuss
		String discussSource = "Discuss";
		ForumThreadParser discuss = new UwantsThreadRetriever("http://www.discuss.com.hk/forumdisplay.php?fid=101&page=2", discussSource);
		result = discuss.parse();		
		checkForumThreadList(discussSource, "www.discuss.com.hk", result);
		
		// Test Tvbox
		String tvboxSource = "Tvboxnow";
		ForumThreadParser tvb = new TvboxnowThreadRetriever("http://www.tvboxnow.com/forum-50-1.html", tvboxSource);
		result = tvb.parse();		
		checkForumThreadList(tvboxSource, "www.tvboxnow.com", result);
	}

	private void checkForumThreadList(String uwantsSource, String urlPrefix, List<ForumThread> result) {
		assertTrue("Return list should not empty", result.size() > 0);
				
		log.info("First ForumThread: {}", result.get(0).toString());
		for (ForumThread f : result) {			
			assertEquals("Source should be same as the one put in constructor", uwantsSource, f.getSource());			
			assertTrue("URL should start with correct prefix", f.getUrl().startsWith(urlPrefix));
			assert StringUtils.isNotBlank(f.getTitle());
		}
	}
		
}
