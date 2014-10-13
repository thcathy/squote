package squote.controller.repository;

import java.math.BigInteger;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import squote.controller.AbstractController;
import squote.controller.ForumController;
import squote.domain.repository.HoldingStockRepository;

@Controller
@RequestMapping("/holdingstock")
public class HoldingStockController extends AbstractController {
	private static Logger log = LoggerFactory.getLogger(HoldingStockController.class);
	
	@Autowired HoldingStockRepository holdingStockRepository;
	
	public HoldingStockController() {
		super("holdingstock");
	}
	
	@RequestMapping("/")
	public String list(ModelMap modelMap) {
		
		modelMap.put("holdingStocks", holdingStockRepository.findAll());
		
		return page("/list");
	}
	
	@RequestMapping("/delete/{id}")
	public String delete(@PathVariable String id, ModelMap modelMap) {
		log.debug("delete: id [{}]", id);
		holdingStockRepository.delete(new BigInteger(id));
		return list(modelMap);
	}
}