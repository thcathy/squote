package squote.controller.rest;

import static squote.SquoteConstants.IndexCode.HSCEI;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import squote.SquoteConstants.IndexCode;
import squote.domain.Fund;
import squote.domain.HoldingStock;
import squote.domain.StockExecutionMessage;
import squote.domain.repository.HoldingStockRepository;
import squote.service.CentralWebQueryService;
import squote.service.UpdateFundByHoldingService;
import squote.web.parser.EtnetIndexQuoteParser;
import squote.web.parser.HSINetParser;

@RequestMapping("/rest/createholding")
@RestController
public class CreateHoldingController {
	private static Logger log = LoggerFactory.getLogger(CreateHoldingController.class);
	
	@Autowired CentralWebQueryService webQueryService;
	@Autowired HoldingStockRepository holdingRepo;
	@Autowired UpdateFundByHoldingService updateFundService;
	
	@RequestMapping(value="/create")
	public Map<String, Object> createHoldingFromExecution(@RequestParam(value="message", required=false, defaultValue="") String exeMsg,
			@RequestParam(value="hscei", required=false, defaultValue="0") String hcei) {
		
		log.debug("createHoldingStockFromExecution: execution msg [{}], hscei [{}]", exeMsg, hcei);
		Optional<StockExecutionMessage> executionMessage = StockExecutionMessage.construct(exeMsg);
		if (!executionMessage.isPresent()) throw new IllegalArgumentException("Cannot create holding"); 
			
		hcei = enrichHscei(hcei, executionMessage.get().getDate());
		HoldingStock holding = HoldingStock.from(executionMessage.get(), new BigDecimal(hcei));
		holdingRepo.save(holding);
		
		HashMap<String, Object> resultMap = new HashMap<String, Object>();
		resultMap.put("holding", holding);
		return resultMap;
	}
	
	@RequestMapping(value="/updatefund")
	public Fund updateFundByHolding(
			@RequestParam(value="fundName", required=true) String fundName,
			@RequestParam(value="holdingId", required=true) String holdingId) {
		return updateFundService.updateFundByHolding(fundName, new BigInteger(holdingId));
	}
	
	private String enrichHscei(String hscei, Date date) {
		if ("0".equals(hscei)) {
			try {
				return parseHsceiFromWeb(date);
			} catch (Exception e) {
				throw new RuntimeException("Cannot get hcei", e);
			}			
		}
		return hscei;
	}
	
	private String parseHsceiFromWeb(Date date) throws Exception {
		String hscei;
		if (DateUtils.isSameDay(new Date(), date)) {
			hscei = parseHSCEIFromEtnet();
		} else {				
			hscei = parseHSCEIFromHSINet(date);
		}
		return hscei.replaceAll(",", "");
	}

	private String parseHSCEIFromEtnet() {
		return webQueryService.parse(new EtnetIndexQuoteParser()).get().stream()
					.filter(a -> IndexCode.HSCEI.name.equals(a.getStockCode())).findFirst().get().getPrice();
	}

	private String parseHSCEIFromHSINet(Date date) {
		return webQueryService.parse(new HSINetParser(HSCEI, date)).get().getPrice();
	}
}
