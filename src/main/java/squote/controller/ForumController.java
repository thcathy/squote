package squote.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import squote.domain.ForumThread;
import squote.domain.VisitedForumThread;
import squote.domain.repository.VisitedForumThreadRepository;
import squote.service.CentralWebQueryService;
import squote.web.parser.ForumThreadParser;

@RequestMapping("/forum")
@Controller
public class ForumController extends AbstractController {
	private static Logger log = LoggerFactory.getLogger(ForumController.class);
	
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
	
	public ForumController() {
		super("forum");
	}
	
	@RequestMapping(value = "/")
	@ResponseBody
	public String index() {
		return "Usage: /list/{type}/{page}";
	}
		
    @RequestMapping(value = "/list/{type}/{page}")
    public String list(@PathVariable String type, @PathVariable int page, ModelMap modelMap) {
    	log.debug("list: type [{}], page [{}]", type, page);

    	List<ForumThreadParser> parsers = getParserByType(ContentType.valueOf(type.toUpperCase()), page);    	   	
    	List<ForumThread> contents = parsers.parallelStream()
											.map(p -> CompletableFuture.supplyAsync(() -> p.parse(), executeService.getExecutor()))
											.map(f -> f.join())
											.flatMap(x->x.stream())
											.sorted((a,b)->b.getCreatedDate().compareTo(a.getCreatedDate()))
											.collect(Collectors.toList());
    	contents.forEach(f->isVisited(f));
    	
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
        
    private List<ForumThreadParser> getParserByType(ContentType type, int page) {    	
    	return type.urls.stream()
    			.map(url -> ForumThreadParser.buildParser(url,page))
    			.collect(Collectors.toList());
    }
}
