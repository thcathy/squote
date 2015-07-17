package squote.controller.rest;

import java.math.BigDecimal;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import squote.domain.Fund;
import squote.domain.repository.FundRepository;

@RequestMapping("/rest/fund")
@RestController
public class FundController {
	private static Logger log = LoggerFactory.getLogger(FundController.class);
		
	@Autowired FundRepository fundRepo;
			
	@RequestMapping(value = "/{fundName}/buy/{code}/{qty}/{gross}")	
	public Fund buy(@PathVariable String fundName, @PathVariable String code, @PathVariable int qty, @PathVariable String gross) {
		log.debug("buy {} {} with gross {} for fund {}", qty, code, gross, fundName);
		Optional<Fund> fund = Optional.of(fundRepo.findOne(fundName));
		fund.ifPresent(f -> {
			f.buyStock(code, qty, new BigDecimal(gross));
			fundRepo.save(f);
		});		
		return fund.get();				
	}	
	
	@RequestMapping(value = "/{fundName}/sell/{code}/{qty}/{gross}")	
	public Fund sell(@PathVariable String fundName, @PathVariable String code, @PathVariable int qty, @PathVariable String gross) {
		log.debug("sell {} {} with gross {} for fund {}", qty, code, gross, fundName);
		Optional<Fund> fund = Optional.of(fundRepo.findOne(fundName));
		fund.ifPresent(f -> {
			f.sellStock(code, qty, new BigDecimal(gross));
			fundRepo.save(f);
		});		
		return fund.get();				
	}
	
	@RequestMapping(value = "/create/{fundName}")
	public Fund create(@PathVariable String fundName) {
		log.debug("Create new fund: {}", fundName);
		return fundRepo.save(new Fund(fundName));
	}
	
	@RequestMapping(value = "/delete/{fundName}")
	public String delete(@PathVariable String fundName) {
		log.debug("Delete fund: {}", fundName);
		fundRepo.delete(fundName);
		return "Done";
	}
	
	
	@RequestMapping(value = "/{fundName}/remove/{code}")
	public Fund removeStock(@PathVariable String fundName, @PathVariable String code) {
		log.debug("remove stock {} from fund {}", code, fundName);
		Fund fund = fundRepo.findOne(fundName);
		fund.getHoldings().remove(code);
		fundRepo.save(fund);
		return fund;
	}
}