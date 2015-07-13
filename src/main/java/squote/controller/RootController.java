package squote.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class RootController {

	@RequestMapping(value = "/robots.txt", method = RequestMethod.GET)
    public String getRobots(HttpServletRequest request) {
        return "robots";
    }
	
	@RequestMapping("/")
	String index() {
		return "index";
	}
	
	@RequestMapping("/fund")
	String fund() {
		return "fund";
	}
}