package squote.controller.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import squote.domain.HoldingStock;
import squote.domain.MarketDailyReport;
import squote.domain.StockQuote;
import squote.domain.repository.HoldingStockRepository;
import squote.service.MarketReportService;
import squote.service.StockPerformanceService;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static squote.service.MarketReportService.pre;

@RestController
@RequestMapping("/rest/stock")
public class RestStockController {
	private static Logger log = LoggerFactory.getLogger(RestStockController.class);

	@Autowired HoldingStockRepository holdingStockRepository;
	@Autowired StockPerformanceService stockPerformanceService;
	@Autowired MarketReportService marketReportService;

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

	@RequestMapping(method = RequestMethod.GET, value = "/liststocksperf")
	public List<StockQuote> listStocksPerformance() throws ExecutionException, InterruptedException {
		return stockPerformanceService.getStockPerformanceQuotes();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/marketreports")
	public Map<String, MarketDailyReport> getMarketDailyReports() {
		List<CompletableFuture<MarketDailyReport>> mktReports = submitMarketDailyReportRequests();

		Map<String, MarketDailyReport> histories = new LinkedHashMap<>();
		histories.put("T", mktReports.get(0).join());
		histories.put("T-1", mktReports.get(1).join());
		histories.put("T-7", mktReports.get(2).join());
		histories.put("T-30", mktReports.get(3).join());
		histories.put("T-60", mktReports.get(4).join());
		return histories;
	}

	private List<CompletableFuture<MarketDailyReport>> submitMarketDailyReportRequests() {
		return marketReportService.getMarketDailyReport(
				pre(1, Calendar.DATE),
				pre(2, Calendar.DATE),
				pre(7, Calendar.DATE),
				pre(30, Calendar.DATE),
				pre(60, Calendar.DATE)
		);
	}
}