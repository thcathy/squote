package squote.controller.rest;

import com.google.common.collect.Lists;
import com.mashape.unirest.http.HttpResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import squote.domain.ForumThread;
import squote.domain.VisitedForumThread;
import squote.domain.WishList;
import squote.domain.repository.VisitedForumThreadRepository;
import squote.domain.repository.WishListRepository;
import squote.service.WebParserRestService;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

@RequestMapping("/rest/forum")
@RestController
public class RestForumController {
	private static Logger log = LoggerFactory.getLogger(RestForumController.class);

	@Autowired public WebParserRestService restService;
	@Autowired public VisitedForumThreadRepository visitedThreadRepo;
	@Autowired public WishListRepository wishListRepo;
	
	@Autowired
	public RestForumController(WebParserRestService restService, VisitedForumThreadRepository visitedThreadRepo, WishListRepository wishListRepo) {
		this.restService = restService;
		this.visitedThreadRepo = visitedThreadRepo;
		this.wishListRepo = wishListRepo;
	}
			
    @RequestMapping(value = "/list/{type}/{batch}")
    public List<ForumThread> list(@PathVariable String type, @PathVariable int batch, ModelMap modelMap) throws Exception {
    	log.debug("list: type [{}], batch [{}]", type, batch);

    	Future<HttpResponse<ForumThread[]>> responseFuture = restService.getForumThreads(type, batch);
    	List<WishList> wishLists = Lists.newArrayList(wishListRepo.findAll());

		List<ForumThread> contents = Arrays.asList(responseFuture.get().getBody());
    	contents.forEach(f -> isWished(f, wishLists));
		contents.forEach(this::isVisited);

		return contents;
    }
    
	@RequestMapping(value = "/visited", method = RequestMethod.POST)
    @ResponseBody
    public void visited(@RequestBody Map<String, String> json) {
		String url = json.get("url");
		String title = json.get("title");
		log.debug("visited: url [{}], title [{}]", url, title);
		if (StringUtils.isBlank(url) && StringUtils.isBlank(title)) throw new IllegalArgumentException("input is blank");
    	
    	visitedThreadRepo.save(new VisitedForumThread(url, title, new Date()));
    }

	@RequestMapping(value = "/wishlist/list")
	public Iterable<WishList> wishlist()
	{
		log.info("get all wishlist");
		return wishListRepo.findAll();
	}

	@RequestMapping(value = "/wishlist/add", method = RequestMethod.POST)
	@ResponseBody
	public WishList addWishList(@RequestBody String text) {
		log.info("add {} to wish list", text);
		return wishListRepo.save(new WishList(text));
	}

	@RequestMapping(value = "/wishlist/delete", method = RequestMethod.POST)
	@ResponseBody
	public Iterable<WishList> delete(@RequestBody String text) {
		log.info("delete {} from wishlist", text);

		wishListRepo.deleteById(text);
		return wishListRepo.findAll();
	}

	private void isVisited(ForumThread f) {
    	if (visitedThreadRepo.existsById(f.getUrl()))
    		f.setVisited(true);
    	else if (visitedThreadRepo.findByTitle(f.getTitle()).size() > 0)
    		f.setVisited(true);
    }

	private void isWished(ForumThread f, List<WishList> wishLists) {
		if (!f.isVisited() && wishLists.stream().anyMatch(t -> f.getTitle().contains(t.text)))
			f.setWished(true);
	}
}
