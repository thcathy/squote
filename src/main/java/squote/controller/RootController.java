package squote.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import squote.domain.repository.FundRepository;

@Controller
public class RootController {
	@Autowired FundRepository fundRepo;

	@RequestMapping(value = "/robots.txt", method = RequestMethod.GET)
    public String getRobots(HttpServletRequest request) {
        return "robots";
    }
	
	@RequestMapping("/")
	String index() {
		return "index";
	}
	
	@RequestMapping("/fund")
	String fund(ModelMap modelMap) {
		modelMap.put("funds", fundRepo.findAll());
		return "fund/fund";
	}
}