package squote.controller.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import squote.domain.Fund;
import squote.domain.FundHolding;
import squote.domain.repository.FundRepository;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.function.BinaryOperator;

@RequestMapping("/rest/fund")
@RestController
public class FundController {
	private static Logger log = LoggerFactory.getLogger(FundController.class);
	
	public enum ValueAction {
		add((x,y) -> x.add(y)),
		subtract((x,y) -> x.subtract(y));
		
		final BinaryOperator<BigDecimal> accumulator;		
		ValueAction(BinaryOperator<BigDecimal> accumulator) {this.accumulator = accumulator;}
	}
		
	@Autowired FundRepository fundRepo;
			
	@RequestMapping(value = "/{fundName}/buy/{code}/{qty}/{price}")	
	public Fund buy(@PathVariable String fundName, @PathVariable String code, @PathVariable int qty, @PathVariable String price) {
		log.debug("buy {} {} with price {} for fund {}", qty, code, price, fundName);
		Fund fund = fundRepo.findOne(fundName);
		fund.buyStock(code, qty, new BigDecimal(qty).multiply(new BigDecimal(price)));
		fundRepo.save(fund);
		log.debug("Updated Fund: {}", fund);
		return fund;				
	}	
	
	@RequestMapping(value = "/{fundName}/sell/{code}/{qty}/{price}")	
	public Fund sell(@PathVariable String fundName, @PathVariable String code, @PathVariable int qty, @PathVariable String price) {
		log.debug("sell {} {} with price {} for fund {}", qty, code, price, fundName);
		Fund fund = fundRepo.findOne(fundName);
		fund.sellStock(code, qty, new BigDecimal(qty).multiply(new BigDecimal(price)));
		fundRepo.save(fund);
		log.debug("Updated Fund: {}", fund);
		return fund;				
	}
	
	@RequestMapping(value = "/create/{fundName}")
	public Fund create(@PathVariable String fundName) {
		log.debug("Create new fund: {}", fundName);
		Fund f = new Fund(fundName);
		f.setProfit(new BigDecimal("0"));
		return fundRepo.save(f);
	}
	
	@RequestMapping(value = "/delete/{fundName}")
	public String delete(@PathVariable String fundName) {
		log.debug("Delete fund: {}", fundName);
		fundRepo.delete(fundName);
		return "{  \"response\" : \"Done\" }";
	}
	
	
	@RequestMapping(value = "/{fundName}/remove/{code}")
	public Fund removeStock(@PathVariable String fundName, @PathVariable String code) {
		log.debug("remove stock {} from fund {}", code, fundName);
		Fund fund = fundRepo.findOne(fundName);
		fund.getHoldings().remove(code);
		fundRepo.save(fund);
		log.debug("Updated Fund: {}", fund);
		return fund;
	}
	
	@RequestMapping(value ="/{fundName}/payinterest/{code}/{amount}")
	public FundHolding payInterest(@PathVariable String fundName, @PathVariable String code, @PathVariable String amount) {
		log.debug("Interest ${} is paid by code {} to fund {}", amount, code, fundName);
		Fund fund = fundRepo.findOne(fundName);
		FundHolding newHolding = fund.payInterest(code, new BigDecimal(amount));
		fundRepo.save(fund);
		log.debug("Updated Fund: {}", fund);
		return newHolding;
	}
	
	@RequestMapping(value = "/{fundName}/set/profit/{amount}")
	public BigDecimal setProfit(@PathVariable String fundName, @PathVariable String amount) {
		log.debug("Set profit of {} to ${}", fundName, amount);
		Fund fund = fundRepo.findOne(fundName);
		fund.setProfit(new BigDecimal(amount));
		fundRepo.save(fund);
		log.debug("Updated Fund: {}", fund);
		return fund.getProfit();
	}
	
	@RequestMapping(value = "/{fundName}/{action}/profit/{amounts}")
	public BigDecimal addProfit(@PathVariable String fundName, @PathVariable ValueAction action, @PathVariable String amounts) {
		log.debug("{} profit '{}' to fund {}", action, amounts, fundName);
		Fund fund = fundRepo.findOne(fundName);
		BigDecimal newProfit = Arrays.stream(amounts.split(","))
			.map(v -> new BigDecimal(v))
			.reduce(fund.getProfit(), action.accumulator);
		fund.setProfit(newProfit);
		fundRepo.save(fund);
		log.debug("Updated Fund: {}", fund);
		return newProfit;
	}
	
	@RequestMapping(value = "/getall")
	public Iterable<Fund> getAll() {
		return fundRepo.findAll();
	}
}