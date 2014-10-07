package squote.controller.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import squote.controller.AbstractController;
import squote.domain.repository.HoldingStockRepository;

@Controller
@RequestMapping("/holdingstock")
public class HoldingStockController extends AbstractController {
	
	@Autowired HoldingStockRepository holdingStockRepository;
	
	public HoldingStockController() {
		super("holdingstock");
	}
	
	@RequestMapping("/")
	public String list(ModelMap modelMap) {
		
		modelMap.put("holdingStocks", holdingStockRepository.findAll());
		
		return page("/list");
	}
	
	
}