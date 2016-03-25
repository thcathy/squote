package squote.controller.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import squote.controller.AbstractController;
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
		modelMap.put("holdingStocks", holdingStockRepository.findAll(new Sort("date")));
		
		return page("/list");
	}
	
	@RequestMapping("/delete/{id}")
	public String delete(@PathVariable String id, ModelMap modelMap) {
		log.warn("delete: id [{}]", id);
		holdingStockRepository.delete(id);
		return list(modelMap);
	}
}