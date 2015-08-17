package squote.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Lists;

import squote.domain.ForumThread;
import squote.domain.VisitedForumThread;
import squote.domain.WishList;
import squote.domain.repository.VisitedForumThreadRepository;
import squote.domain.repository.WishListRepository;
import squote.service.CentralWebQueryService;
import squote.web.parser.ForumThreadParser;

@RequestMapping("/forum")
@Controller
public class ForumController extends AbstractController {	
	private static Logger log = LoggerFactory.getLogger(ForumController.class);
	private Date earliestCreatedDate;
	private int pagePerBatch;
		
	enum ContentType {
		MUSIC(new String[]{
				"http://www.uwants.com/forumdisplay.php?fid=472&page=%d",
				"http://www.uwants.com/forumdisplay.php?fid=471&page=%d",
				"http://www.discuss.com.hk/forumdisplay.php?fid=101&page=%d",
				"http://www.tvboxnow.com/forum-50-%d.html",
				"http://www.tvboxnow.com/forum-153-%d.html"
		}), 
		MOVIE(new String[]{
				"http://www.uwants.com/forumdisplay.php?fid=231&page=%d",
				"http://www.uwants.com/forumdisplay.php?fid=7&page=%d",
				"http://www.uwants.com/forumdisplay.php?fid=406&page=%d",
				"http://www.tvboxnow.com/forum-231-%d.html",
				"http://www.tvboxnow.com/forum-232-%d.html",
				"http://www.tvboxnow.com/forum-233-%d.html"
		});
		
		final private List<String> urls;
		
		ContentType(String[] urls) { 
			this.urls = Collections.unmodifiableList(Arrays.asList(urls));
		}
	}
	
	@Resource private CentralWebQueryService executeService;
	@Resource private VisitedForumThreadRepository visitedThreadRepo;
	@Resource private WishListRepository wishListRepo;
	
	@Autowired
	public ForumController(@Value("${forum.threadEarliestDay}") int threadShouldNotOlderDay, @Value("${forum.pagePerBatch}") int pagePerBatch) {
		super("forum");
		earliestCreatedDate = DateUtils.addDays(new Date(), -threadShouldNotOlderDay);
		this.pagePerBatch = pagePerBatch;
	}
			
    @RequestMapping(value = "/list/{type}/{batch}")
    public String list(@PathVariable String type, @PathVariable int batch, ModelMap modelMap) {
    	log.debug("list: type [{}], batch [{}]", type, batch);

    	List<ForumThreadParser> parsers = getParserByType(ContentType.valueOf(type.toUpperCase()), batch);    	   	
    	List<ForumThread> contents = collectSortedForumThread(parsers);
    	contents.forEach(this::isVisited);
    	
    	List<WishList> wishLists = Lists.newArrayList(wishListRepo.findAll());
    	contents.forEach(f -> isWished(f, wishLists));
    	
		modelMap.put("contents", contents);    	
    	return page("/list");
    }
    
	@RequestMapping(value = "/visited", method = RequestMethod.POST)
    @ResponseBody
    public void visited(@RequestBody String url) {    	
    	log.debug("visited: url [{}]", url);
    	if (StringUtils.isBlank(url)) throw new IllegalArgumentException("input url is blank");
    	
    	visitedThreadRepo.save(new VisitedForumThread(url, new Date()));
    }
    
    private void isVisited(ForumThread f) {
    	if (visitedThreadRepo.exists(f.getUrl())) f.setVisited(true);
    }
    
    
        
    private List<ForumThreadParser> getParserByType(ContentType type, int batch) {
    	return type.urls.stream()
    			.flatMap(url -> (Stream<ForumThreadParser>) 
    					IntStream.rangeClosed(fromPage(batch), toPage(batch)).boxed()
    							.map(i -> ForumThreadParser.buildParser(url,i))
    			)
    			.collect(Collectors.toList());    			
    }
    
    private List<ForumThread> collectSortedForumThread(
			List<ForumThreadParser> parsers) {
		return parsers.parallelStream()
				.map(p -> CompletableFuture.supplyAsync(() -> p.parse(), executeService.getExecutor()))
				.map(CompletableFuture::join)
				.flatMap(List::stream)
				.filter(f -> f.getCreatedDate().compareTo(earliestCreatedDate) >= 0)
				.sorted((a,b)->b.getCreatedDate().compareTo(a.getCreatedDate()))
				.collect(Collectors.toList());
	}

	private int toPage(int batch) {
		return pagePerBatch * batch;
	}

	private int fromPage(int batch) {
		return 1 + (pagePerBatch * (batch-1));
	}
	
	private void isWished(ForumThread f, List<WishList> wishLists) {
		if (wishLists.stream().anyMatch(t -> f.getTitle().contains(t.text)))
			f.setWished(true);
	}
}
