package squote.controller.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import squote.domain.HoldingStock;
import squote.domain.repository.HoldingStockRepository;

@RestController
@RequestMapping("/rest/stock")
public class RestStockController {
	private static Logger log = LoggerFactory.getLogger(RestStockController.class);

	@Autowired HoldingStockRepository holdingStockRepository;

	@RequestMapping("/holding/list")
	public Iterable<HoldingStock> listHolding() {
		log.info("list holding");
		return holdingStockRepository.findAll(new Sort("date"));
	}
	
	@RequestMapping("/holding/delete/{id}")
	public Iterable<HoldingStock> delete(@PathVariable String id) {
		log.warn("delete: id [{}]", id);
		holdingStockRepository.delete(id);
		return holdingStockRepository.findAll(new Sort("date"));
	}
}