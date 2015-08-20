package squote.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
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

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = SpringQuoteWebApplication.class)
@ActiveProfiles("dev")
public class ForumControllerTest {
	private static final String WISHLIST_ITEM2 = "歌詞";
	private static final String WISHLIST_ITEM1 = "320K MP3";
	@Resource ForumController controller;
	@Resource VisitedForumThreadRepository visitedRepo;
	@Resource WishListRepository wishListRepo;
	@Value("${forum.threadEarliestDay}") int threadShouldNotOlderDay;
	
	private static int MIN_NUM_OF_THREADS = 200;
			
	@Test
	public void list_MusicPage1_ShouldReturnDecendingForumThreadsNotOlderThanConfig() {
		ModelMap modelMap = new ModelMap();
		controller.list("MUSIC", 1, modelMap);
		@SuppressWarnings("unchecked") List<ForumThread> contents = (List<ForumThread>) modelMap.get("contents");
		
		assertTrue("Number of thread " + contents.size() + " < " + MIN_NUM_OF_THREADS, contents.size() > MIN_NUM_OF_THREADS);
		boolean descSortedByDate = IntStream.range(0, contents.size()-1)
									.allMatch(i -> contents.get(i).getCreatedDate().getTime() >= contents.get(i+1).getCreatedDate().getTime());
		assertTrue("Contents are decending ordered by created date", descSortedByDate);
		contents.forEach(x -> {
			assert StringUtils.isNotBlank(x.getUrl());			
			assert StringUtils.isNotBlank(x.getTitle());
		});
		
		Date earliestCreatedDate = DateUtils.addDays(new Date(), -threadShouldNotOlderDay);
		contents.forEach(x -> {
			assertTrue("The thread created on " + x.getCreatedDate() + " is older than " + threadShouldNotOlderDay + " days", x.getCreatedDate().compareTo(earliestCreatedDate) >= 0); 
		});
	}
	
	@Test
	public void list_givenWishListItem_shouldMarkItemAsWished() {
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
	public void list_MoviePage1_ShouldReturnDecendingForumThreadsNotOlderThanConfig() {
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
		
		Date earliestCreatedDate = DateUtils.addDays(new Date(), -threadShouldNotOlderDay);
		contents.forEach(x -> {
			assertTrue("The thread created on " + x.getCreatedDate() + " is older than " + threadShouldNotOlderDay + " days", x.getCreatedDate().compareTo(earliestCreatedDate) >= 0); 
		});
	}
	
	@Test
	public void visited_GivenUrl_ShouldCreateEntityVisitedForumThread() {
		String url = "http://www.uwants.com/viewthread.php?tid=18017060&extra=page%3D1";
		controller.visited(url);
		VisitedForumThread t = visitedRepo.findOne(url);
		assertEquals(t.getUrl(),url);		
		visitedRepo.delete(t);
	}
	
	private boolean isWishList(ForumThread f) {
		return f.getTitle().contains(WISHLIST_ITEM1) || f.getTitle().contains(WISHLIST_ITEM2);  
	}
}
