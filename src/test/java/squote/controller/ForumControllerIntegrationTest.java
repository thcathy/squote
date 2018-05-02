package squote.controller;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.ui.ModelMap;
import squote.SpringQuoteWebApplication;
import squote.domain.ForumThread;
import squote.domain.VisitedForumThread;
import squote.domain.WishList;
import squote.domain.repository.VisitedForumThreadRepository;
import squote.domain.repository.WishListRepository;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = SpringQuoteWebApplication.class)
@ActiveProfiles("dev")
public class ForumControllerIntegrationTest {
	private static final String WISHLIST_ITEM2 = "歌詞";
	private static final String WISHLIST_ITEM1 = "320K MP3";

	@Autowired VisitedForumThreadRepository visitedRepo;
	@Autowired WishListRepository wishListRepo;
	@Autowired ForumController controller;

	@Test
	public void list_givenWishListItem_shouldMarkItemAsWished() throws Exception {
		wishListRepo.save(new WishList(WISHLIST_ITEM1));
		wishListRepo.save(new WishList(WISHLIST_ITEM2));
		
		try {
			ModelMap modelMap = new ModelMap();
			controller.list("MUSIC", 1, modelMap);
			List<ForumThread> contents = (List<ForumThread>) modelMap.get("contents");
			contents.stream()
				.filter(this::isWishList)
				.forEach(x -> assertTrue("Forum Thread should be true: " + x, x.isVisited() || x.isWished()));			
		} finally {
			wishListRepo.deleteAll();
		}
	}
	
	@Test
	public void list_MoviePage1_ShouldReturnDecendingForumThreadsNotOlderThanConfig() throws Exception {
		ModelMap modelMap = new ModelMap();
		controller.list("MOVIE", 1, modelMap);
		@SuppressWarnings("unchecked") List<ForumThread> contents = (List<ForumThread>) modelMap.get("contents");
		
		assertTrue("Number of thread " + contents.size() + " < " + 50, contents.size() > 50);
		boolean descSortedByDate = IntStream.range(0, contents.size()-1)
									.allMatch(i -> contents.get(i).getCreatedDate().getTime() >= contents.get(i+1).getCreatedDate().getTime());
		assertTrue("Contents are decending ordered by created date", descSortedByDate);
		contents.forEach(x -> {
			assert StringUtils.isNotBlank(x.getUrl());			
			assert StringUtils.isNotBlank(x.getTitle());
		});

	}
	
	@Test
	public void visited_GivenUrl_ShouldCreateEntityVisitedForumThread() {
		String url = "http://www.uwants.com/viewthread.php?tid=18017060&extra=page%3D1";
		String title = "content title";
		controller.visited(url, title);
		VisitedForumThread t = visitedRepo.findOne(url);
		assertEquals(t.getUrl(), url);
		assertEquals(t.getTitle(), title);
		visitedRepo.delete(t);
	}

	@Test
	public void visited_GivenTitle_ShouldCreateEntityVisitedForumThread() {
		String url = "old url";
		String title = "content title";
		controller.visited(url, title);
		VisitedForumThread t = visitedRepo.findByTitle(title).get(0);
		assertEquals(t.getUrl(), url);
		assertEquals(t.getTitle(), title);
		visitedRepo.delete(t);
	}
	
	private boolean isWishList(ForumThread f) {
		return f.getTitle().contains(WISHLIST_ITEM1) || f.getTitle().contains(WISHLIST_ITEM2);  
	}
}
