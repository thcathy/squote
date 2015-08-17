package squote.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.ModelMap;

import com.google.common.collect.Lists;

import squote.SpringQuoteWebApplication;
import squote.controller.repository.WishListController;
import squote.domain.ForumThread;
import squote.domain.VisitedForumThread;
import squote.domain.WishList;
import squote.domain.repository.VisitedForumThreadRepository;
import squote.domain.repository.WishListRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = SpringQuoteWebApplication.class)
@ActiveProfiles("dev")
public class WishListControllerTest {
	@Autowired WishListController controller;
	@Autowired WishListRepository wishlistRepo;
	
	private MockMvc mockMvc;

	@Before
	public void setup() {
		this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
		createWishListItems();
	}

	@After
	public void clearup() {
		wishlistRepo.deleteAll();
	}

	@Test
	public void wishlist_givenNoNewItem_shouldReturnSameNumberOfItems() throws Exception {
		ModelMap modelMap = mockMvc.perform(get("/forum/wishlist/"))
			.andExpect(status().isOk())
			.andExpect(view().name("forum/wishlist"))
			.andReturn().getModelAndView().getModelMap();
				
		List<WishList> items = Lists.newArrayList((Iterable<WishList>) modelMap.get("items"));
		assertEquals(3, items.size());
	}
	
	@Test
	public void wishlist_givenNewItem_shouldReturnOneMoreItems() throws Exception {
		String newItemText = "item new 1";
		ModelMap modelMap = mockMvc.perform(get("/forum/wishlist/").param("newItem", newItemText))
				.andExpect(status().isOk())
				.andExpect(view().name("forum/wishlist"))
				.andReturn().getModelAndView().getModelMap();
					
			List<WishList> items = Lists.newArrayList((Iterable<WishList>) modelMap.get("items"));
			assertEquals(4, items.size());
			assertEquals(newItemText, wishlistRepo.findOne(newItemText).text);
	}
	
	@Test
	public void delete_givenNoItem_shouldReturnSameNumberOfItems() throws Exception {
		ModelMap modelMap = mockMvc.perform(get("/forum/wishlist/delete/"))
				.andExpect(status().isOk())
				.andExpect(view().name("forum/wishlist"))
				.andReturn().getModelAndView().getModelMap();
					
			List<WishList> items = Lists.newArrayList((Iterable<WishList>) modelMap.get("items"));
			assertEquals(3, items.size());
	}
	
	@Test
	public void delete_givenItem_shouldBeRemoved() throws Exception {
		ModelMap modelMap = mockMvc.perform(get("/forum/wishlist/delete?item=item 2"))
				.andExpect(status().isOk())
				.andExpect(view().name("forum/wishlist"))
				.andReturn().getModelAndView().getModelMap();
					
			List<WishList> items = Lists.newArrayList((Iterable<WishList>) modelMap.get("items"));
			assertEquals(2, items.size());
			assertEquals(null, wishlistRepo.findOne("item 2"));
	}
	
	private void createWishListItems() {
		wishlistRepo.save(new WishList("item 1"));
		wishlistRepo.save(new WishList("item 2"));
		wishlistRepo.save(new WishList("item 3"));
	}
}
