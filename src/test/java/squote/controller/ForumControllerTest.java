package squote.controller;

import static org.junit.Assert.assertTrue;

import java.util.List;

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

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = SpringQuoteWebApplication.class)
@ActiveProfiles("dev")
public class ForumControllerTest {
	@Resource ForumController controller;
	
	@Test
	public void getMusicForumThreads() {
		ModelMap modelMap = new ModelMap();
		controller.list("MUSIC", 1, modelMap);
		List<ForumThread> contents = (List<ForumThread>) modelMap.get("contents");
		
		assertTrue("Number of thread return should > 40", contents.size() > 40);
		for (ForumThread t : contents) {
			assert StringUtils.isNotBlank(t.getUrl());			
			assert StringUtils.isNotBlank(t.getTitle());
		}
	}
}
