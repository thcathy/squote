package squote.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import squote.domain.ForumThread;
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
	
		modelMap.put("contents", contents);    	
    	return page("/list");
    }
        
    private List<ForumThreadParser> getParserByType(ContentType type, int page) {    	
    	return type.urls.stream()
    			.map(url -> ForumThreadParser.buildParser(url,page))
    			.collect(Collectors.toList());
    }
}
