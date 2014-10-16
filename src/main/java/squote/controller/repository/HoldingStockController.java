package squote.controller.repository;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
import squote.domain.HoldingStock;
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
		
		List<HoldingStock> sortedHoldingStocks = StreamSupport.stream(holdingStockRepository.findAll().spliterator(), true)
			.sorted( (x,y)->x.getDate().compareTo(y.getDate()) )
			.collect(Collectors.toList());
		
		modelMap.put("holdingStocks", sortedHoldingStocks);
		
		return page("/list");
	}
	
	@RequestMapping("/delete/{id}")
	public String delete(@PathVariable String id, ModelMap modelMap) {
		log.debug("delete: id [{}]", id);
		holdingStockRepository.delete(new BigInteger(id));
		return list(modelMap);
	}
}