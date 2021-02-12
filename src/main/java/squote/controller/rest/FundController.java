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
import squote.security.AuthenticationService;

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
	@Autowired AuthenticationService authenticationService;
			
	@RequestMapping(value = "/{fundName}/buy/{code}/{qty}/{price}")	
	public Fund buy(@PathVariable String fundName, @PathVariable String code, @PathVariable int qty, @PathVariable String price) {
		String userId = authenticationService.getUserId().get();
		log.debug("buy {} {} with price {} for fund {}:{}", qty, code, price, userId, fundName);
		Fund fund = fundRepo.findByUserIdAndName(userId, fundName).get();
		fund.buyStock(code, qty, new BigDecimal(qty).multiply(new BigDecimal(price)));
		fundRepo.save(fund);
		log.debug("Updated Fund: {}", fund);
		return fund;				
	}	
	
	@RequestMapping(value = "/{fundName}/sell/{code}/{qty}/{price}")	
	public Fund sell(@PathVariable String fundName, @PathVariable String code, @PathVariable int qty, @PathVariable String price) {
		String userId = authenticationService.getUserId().get();
		log.debug("sell {} {} with price {} for fund {}:{}", qty, code, price, userId, fundName);
		Fund fund = fundRepo.findByUserIdAndName(userId, fundName).get();
		fund.sellStock(code, qty, new BigDecimal(qty).multiply(new BigDecimal(price)));
		fundRepo.save(fund);
		log.debug("Updated Fund: {}", fund);
		return fund;				
	}
	
	@RequestMapping(value = "/create/{fundName}")
	public Fund create(@PathVariable String fundName) {
		String userId = authenticationService.getUserId().get();
		log.debug("Create new fund: {}:{}", userId, fundName);
		Fund f = new Fund(userId, fundName);
		f.setProfit(new BigDecimal("0"));
		return fundRepo.save(f);
	}
	
	@RequestMapping(value = "/delete/{fundName}")
	public String delete(@PathVariable String fundName) {
		String userId = authenticationService.getUserId().get();
		log.debug("Delete fund: {}:{}", userId, fundName);
		fundRepo.findByUserIdAndName(userId, fundName)
			.ifPresent(f -> fundRepo.delete(f));

		return "{  \"response\" : \"Done\" }";
	}
	
	
	@RequestMapping(value = "/{fundName}/remove/{code}")
	public Fund removeStock(@PathVariable String fundName, @PathVariable String code) {
		String userId = authenticationService.getUserId().get();
		log.debug("remove stock {} from fund {}:{}", code, userId, fundName);
		Fund fund = fundRepo.findByUserIdAndName(userId, fundName).get();
		fund.getHoldings().remove(code);
		fundRepo.save(fund);
		log.debug("Updated Fund: {}", fund);
		return fund;
	}
	
	@RequestMapping(value ="/{fundName}/payinterest/{code}/{amount}")
	public FundHolding payInterest(@PathVariable String fundName, @PathVariable String code, @PathVariable String amount) {
		String userId = authenticationService.getUserId().get();
		log.debug("Interest ${} is paid by code {} to fund {}:{}", amount, code, userId, fundName);
		Fund fund = fundRepo.findByUserIdAndName(userId, fundName).get();
		FundHolding newHolding = fund.payInterest(code, new BigDecimal(amount));
		fundRepo.save(fund);
		log.debug("Updated Fund: {}", fund);
		return newHolding;
	}
	
	@RequestMapping(value = "/{fundName}/set/profit/{amount}")
	public BigDecimal setProfit(@PathVariable String fundName, @PathVariable String amount) {
		String userId = authenticationService.getUserId().get();
		log.debug("Set profit of {}:{} to ${}", userId, fundName, amount);
		Fund fund = fundRepo.findByUserIdAndName(userId, fundName).get();
		fund.setProfit(new BigDecimal(amount));
		fundRepo.save(fund);
		log.debug("Updated Fund: {}", fund);
		return fund.getProfit();
	}
	
	@RequestMapping(value = "/{fundName}/{action}/profit/{amounts}")
	public BigDecimal addProfit(@PathVariable String fundName, @PathVariable ValueAction action, @PathVariable String amounts) {
		String userId = authenticationService.getUserId().get();
		log.debug("{} profit '{}' to fund {}:{}", action, amounts, userId, fundName);
		Fund fund = fundRepo.findByUserIdAndName(userId, fundName).get();
		BigDecimal newProfit = Arrays.stream(amounts.split(","))
			.map(v -> new BigDecimal(v))
			.reduce(fund.getProfit(), action.accumulator);
		fund.setProfit(newProfit);
		fundRepo.save(fund);
		log.debug("Updated Fund: {}", fund);
		return newProfit;
	}

	@RequestMapping(value = "/{fundName}/cashout/{amount}")
	public Fund cashOut(@PathVariable String fundName, @PathVariable String amount) {
		String userId = authenticationService.getUserId().get();
		log.debug("cashout {} from fund {}:{}", amount, userId, fundName);
		Fund fund = fundRepo.findByUserIdAndName(userId, fundName).get();
		fund.cashout(new BigDecimal(amount));
		fundRepo.save(fund);
		log.debug("Updated Fund: {}", fund);
		return fund;
	}
	
	@RequestMapping(value = "/getall")
	public Iterable<Fund> getAll() {
		return fundRepo.findByUserId(authenticationService.getUserId().get());
	}
}
