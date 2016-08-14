package squote.controller.rest;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import squote.domain.Fund;
import squote.domain.HoldingStock;
import squote.domain.StockExecutionMessage;
import squote.domain.repository.HoldingStockRepository;
import squote.service.UpdateFundByHoldingService;
import squote.service.WebParserRestService;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static squote.SquoteConstants.IndexCode.HSCEI;

@RequestMapping("/rest/createholding")
@RestController
public class CreateHoldingController {
	private static Logger log = LoggerFactory.getLogger(CreateHoldingController.class);

	@Autowired HoldingStockRepository holdingRepo;
	@Autowired UpdateFundByHoldingService updateFundService;
	@Autowired WebParserRestService webService;
	
	@RequestMapping(value="/create")
	public HoldingStock createHoldingFromExecution(@RequestParam(value="message", required=false, defaultValue="") String exeMsg,
			@RequestParam(value="hscei", required=false, defaultValue="0") String hcei) {
		
		log.debug("createHoldingStockFromExecution: execution msg [{}], hscei [{}]", exeMsg, hcei);
		Optional<StockExecutionMessage> executionMessage = StockExecutionMessage.construct(exeMsg);
		if (!executionMessage.isPresent()) throw new IllegalArgumentException("Cannot create holding"); 
			
		hcei = enrichHscei(hcei, executionMessage.get().getDate());
		HoldingStock holding = HoldingStock.from(executionMessage.get(), new BigDecimal(hcei));
		holdingRepo.save(holding);
	
		return holding;
	}
	
	@RequestMapping(value="/updatefund")
	public Fund updateFundByHolding(
			@RequestParam(value="fundName", required=true) String fundName,
			@RequestParam(value="holdingId", required=true) String holdingId) {
		log.info("updateFundByHolding: add holding {} to fund {}", holdingId, fundName);
		
		return updateFundService.updateFundByHoldingAndPersist(fundName, holdingId);
	}
	
	private String enrichHscei(String hscei, Date date) {
		if ("0".equals(hscei)) {
			try {
				return parseHsceiFromWeb(date);
			} catch (Exception e) {
				throw new RuntimeException("Cannot getHistoryPrice hcei", e);
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

	private String parseHSCEIFromEtnet() throws ExecutionException, InterruptedException {
		return Arrays.stream(webService.getIndexQuotes().get().getBody())
				.filter(a -> HSCEI.toString().equals(a.getStockCode()))
				.findFirst()
				.get()
				.getPrice();
	}

	private String parseHSCEIFromHSINet(Date date) throws ExecutionException, InterruptedException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

		return Arrays.stream(webService.getHSINetReports(dateFormat.format(date)).get().getBody())
				.filter(a -> HSCEI.toString().equals(a.getStockCode()))
				.findFirst()
				.get()
				.getPrice();
	}
}
