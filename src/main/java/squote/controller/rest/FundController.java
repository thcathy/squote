package squote.controller.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import squote.domain.AlgoConfig;
import squote.domain.Fund;
import squote.domain.FundHolding;
import squote.domain.repository.FundRepository;
import squote.security.AuthenticationService;
import squote.service.SplitInterestService;
import squote.service.UpdateFundByHoldingService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BinaryOperator;

@RequestMapping("/rest/fund")
@RestController
public class FundController {
	private static Logger log = LoggerFactory.getLogger(FundController.class);
	public static final String SEPARATOR = ",";

	public enum ValueAction {
		add((x,y) -> x.add(y)),
		subtract((x,y) -> x.subtract(y));
		
		final BinaryOperator<BigDecimal> accumulator;		
		ValueAction(BinaryOperator<BigDecimal> accumulator) {this.accumulator = accumulator;}
	}
		
	@Autowired FundRepository fundRepo;
	@Autowired AuthenticationService authenticationService;
	@Autowired UpdateFundByHoldingService updateFundByHoldingService;
	@Autowired SplitInterestService splitInterestService;
			
	@RequestMapping(value = "/{fundName}/buy/{code}/{qty}/{price}")
	public Fund buy(@PathVariable String fundName, @PathVariable String code, @PathVariable String qty, @PathVariable String price) {
		String userId = authenticationService.getUserId().get();
		log.info("buy {} {} with price {} for fund {}:{}", qty, code, price, userId, fundName);
		Fund fund = fundRepo.findByUserIdAndName(userId, fundName).get();
		BigDecimal qtyValue = new BigDecimal(qty);
		fund.buyStock(code, qtyValue, qtyValue.multiply(new BigDecimal(price)));
		fundRepo.save(fund);
		log.info("Updated Fund: {}", fund);
		return fund;				
	}	
	
	@RequestMapping(value = "/{fundName}/sell/{code}/{qty}/{price}")
	public Fund sell(@PathVariable String fundName, @PathVariable String code, @PathVariable String qty, @PathVariable String price) {
		String userId = authenticationService.getUserId().get();
		log.info("sell {} {} with price {} for fund {}:{}", qty, code, price, userId, fundName);
		Fund fund = fundRepo.findByUserIdAndName(userId, fundName).get();
		BigDecimal qtyValue = new BigDecimal(qty);
		fund.sellStock(code, qtyValue, qtyValue.multiply(new BigDecimal(price)));
		fundRepo.save(fund);
		log.info("Updated Fund: {}", fund);
		return fund;				
	}
	
	@RequestMapping(value = "/create/{fundName}/{type}")
	public Fund create(@PathVariable String fundName, @PathVariable Fund.FundType type) {
		String userId = authenticationService.getUserId().get();
		log.info("Create new fund: {}:{} ({})", userId, fundName, type);
		Fund f = new Fund(userId, fundName);
		f.setType(type);
		f.setProfit(new BigDecimal("0"));
		return fundRepo.save(f);
	}
	
	@RequestMapping(value = "/delete/{fundName}")
	public String delete(@PathVariable String fundName) {
		String userId = authenticationService.getUserId().get();
		log.info("Delete fund: {}:{}", userId, fundName);
		fundRepo.findByUserIdAndName(userId, fundName)
			.ifPresent(f -> fundRepo.delete(f));

		return "{  \"response\" : \"Done\" }";
	}
	
	
	@RequestMapping(value = "/{fundName}/remove/{code}")
	public Fund removeStock(@PathVariable String fundName, @PathVariable String code) {
		String userId = authenticationService.getUserId().get();
		log.info("remove stock {} from fund {}:{}", code, userId, fundName);
		Fund fund = fundRepo.findByUserIdAndName(userId, fundName).get();
		fund.getHoldings().remove(code);
		fundRepo.save(fund);
		log.info("Updated Fund: {}", fund);
		return fund;
	}
	
	@RequestMapping(value ="/{fundName}/payinterest/{code}/{amount}")
	public FundHolding payInterest(@PathVariable String fundName, @PathVariable String code, @PathVariable String amount) {
		String userId = authenticationService.getUserId().get();
		log.info("Interest ${} is paid by code {} to fund {}:{}", amount, code, userId, fundName);
		Fund fund = fundRepo.findByUserIdAndName(userId, fundName).get();
		FundHolding newHolding = fund.payInterest(code, new BigDecimal(amount));
		fundRepo.save(fund);
		log.info("Updated Fund: {}", fund);
		return newHolding;
	}

	@GetMapping(value = "/splitinterest/{code}/{amount}/{*fundNames}")
	public List<String> splitInterest(@PathVariable String code, @PathVariable String amount, @PathVariable String fundNames) {
		String userId = authenticationService.getUserId().get();
		log.info("Interest ${} is paid by code {} to funds {}:{}", amount, code, userId, fundNames);

		return splitInterestService.splitInterest(userId, code, amount, fundNames.substring(1).split("/"));
	}
	
	@RequestMapping(value = "/{fundName}/set/profit/{amount}")
	public BigDecimal setProfit(@PathVariable String fundName, @PathVariable String amount) {
		String userId = authenticationService.getUserId().get();
		log.info("Set profit of {}:{} to ${}", userId, fundName, amount);
		Fund fund = fundRepo.findByUserIdAndName(userId, fundName).get();
		fund.setProfit(new BigDecimal(amount));
		fundRepo.save(fund);
		log.info("Updated Fund: {}", fund);
		return fund.getProfit();
	}
	
	@RequestMapping(value = "/{fundName}/{action}/profit/{amounts}")
	public BigDecimal addProfit(@PathVariable String fundName, @PathVariable ValueAction action, @PathVariable String amounts) {
		String userId = authenticationService.getUserId().get();
		log.info("{} profit '{}' to fund {}:{}", action, amounts, userId, fundName);
		Fund fund = fundRepo.findByUserIdAndName(userId, fundName).get();
		BigDecimal newProfit = Arrays.stream(amounts.split(SEPARATOR))
			.map(v -> new BigDecimal(v))
			.reduce(fund.getProfit(), action.accumulator);
		fund.setProfit(newProfit);
		fundRepo.save(fund);
		log.info("Updated Fund: {}", fund);
		return newProfit;
	}

	@RequestMapping(value = "/{fundName}/cashout/{amount}")
	public Fund cashOut(@PathVariable String fundName, @PathVariable String amount) {
		String userId = authenticationService.getUserId().get();
		log.info("cashout {} from fund {}:{}", amount, userId, fundName);
		Fund fund = fundRepo.findByUserIdAndName(userId, fundName).get();
		fund.cashout(new BigDecimal(amount));
		fundRepo.save(fund);
		log.info("Updated Fund: {}", fund);
		return fund;
	}

	@RequestMapping(value = "/{fundName}/cashin/{amount}")
	public Fund cashIn(@PathVariable String fundName, @PathVariable String amount) {
		String userId = authenticationService.getUserId().get();
		log.info("cashin {} from fund {}:{}", amount, userId, fundName);
		Fund fund = fundRepo.findByUserIdAndName(userId, fundName).get();
		fund.cashin(new BigDecimal(amount));
		fundRepo.save(fund);
		log.info("Updated Fund: {}", fund);
		return fund;
	}
	
	@RequestMapping(value = "/getall")
	public Iterable<Fund> getAll() {
		return fundRepo.findByUserId(authenticationService.getUserId().get());
	}

	@RequestMapping(value = "/{fundName}/type/{type}")
	public Fund setType(@PathVariable String fundName, @PathVariable Fund.FundType type) {
		String userId = authenticationService.getUserId().get();
		log.info("set fund {} type = {}", fundName, type);
		Fund fund = fundRepo.findByUserIdAndName(userId, fundName).get();
		fund.setType(type);
		fundRepo.save(fund);
		log.info("Updated Fund: {}", fund);
		return fund;
	}

	@RequestMapping(value = "/{fundName}/get-trades/from/{source}")
	public Fund getTrades(@PathVariable String fundName, @PathVariable String source) {
		String userId = authenticationService.getUserId().get();
		log.info("get trades from {} from fund {}", source, fundName);
		return updateFundByHoldingService.getTradesAndUpdateFund(userId, fundName, source);
	}

	@GetMapping(value = "/{fundName}/algo")
	public List<AlgoConfig> getAllAlgoConfigs(@PathVariable String fundName) {
		String userId = authenticationService.getUserId().get();
		log.info("get all algo configs from fund {}", fundName);
		Fund fund = fundRepo.findByUserIdAndName(userId, fundName).get();
		log.info("{} configs return", fund.getAlgoConfigs().size());
		return new ArrayList<>(fund.getAlgoConfigs().values());
	}

	@GetMapping(value = "/{fundName}/algo/{code}")
	public AlgoConfig addOrUpdateAlgoConfig(@PathVariable String fundName, @PathVariable String code,
											@RequestParam(name = "quantity", required = false) int quantity,
											@RequestParam(name = "basePrice", required = false) Double basePrice,
											@RequestParam(name = "stdDevRange", defaultValue = "11") int stdDevRange,
											@RequestParam(name = "stdDevMultiplier", defaultValue = "0.7") double stdDevMultiplier) {
		var userId = authenticationService.getUserId().get();
		
		var newConfig = new AlgoConfig(code, quantity, basePrice, stdDevRange, stdDevMultiplier);
		log.info("add or update algo config {} to fund {}", newConfig, fundName);

		var fund = fundRepo.findByUserIdAndName(userId, fundName).get();
		fund.getAlgoConfigs().put(code, newConfig);
		fundRepo.save(fund);
		log.info("Updated algos: {}", fund.getAlgoConfigs());
		return newConfig;
	}
	
	@DeleteMapping(value = "/{fundName}/algo/{code}")
	public void deleteAlgoConfig(@PathVariable String fundName, @PathVariable String code) {
		var userId = authenticationService.getUserId().get();
		log.info("remove algo config {} from fund {}", code, fundName);

		var fund = fundRepo.findByUserIdAndName(userId, fundName).get();
		fund.getAlgoConfigs().remove(code);
		fundRepo.save(fund);
		log.info("Updated algos: {}", fund.getAlgoConfigs());
	}
}
