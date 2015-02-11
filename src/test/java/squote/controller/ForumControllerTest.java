package squote.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.IntStream;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.ui.ModelMap;

import squote.SpringQuoteWebApplication;
import squote.domain.ForumThread;
import squote.domain.VisitedForumThread;
import squote.domain.repository.VisitedForumThreadRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = SpringQuoteWebApplication.class)
@ActiveProfiles("dev")
public class ForumControllerTest {
	@Resource ForumController controller;
	@Resource VisitedForumThreadRepository visitedRepo;
		
	@Test
	public void getMusicForumThreads() {
		ModelMap modelMap = new ModelMap();
		controller.list("MUSIC", 1, modelMap);
		@SuppressWarnings("unchecked") List<ForumThread> contents = (List<ForumThread>) modelMap.get("contents");
		
		assertTrue("Number of thread return should > 40", contents.size() > 40);
		boolean descSortedByDate = IntStream.range(0, contents.size()-1)
									.allMatch(i -> contents.get(i).getCreatedDate().getTime() >= contents.get(i+1).getCreatedDate().getTime());
		assertTrue("Contents are decending ordered by created date", descSortedByDate);
		contents.forEach(x -> {
			assert StringUtils.isNotBlank(x.getUrl());			
			assert StringUtils.isNotBlank(x.getTitle());
		});
	}
	
	@Test
	public void saveVisitedForumThread() {
		String url = "http://www.uwants.com/viewthread.php?tid=18017060&extra=page%3D1";
		controller.visited(url);
		VisitedForumThread t = visitedRepo.findOne(url);
		assertEquals(t.getUrl(),url);		
		visitedRepo.delete(t);
	}
}
