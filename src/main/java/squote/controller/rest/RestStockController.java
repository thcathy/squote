package squote.controller.rest;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mashape.unirest.http.HttpResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import squote.domain.*;
import squote.domain.repository.FundRepository;
import squote.domain.repository.HoldingStockRepository;
import squote.domain.repository.StockQueryRepository;
import squote.service.MarketReportService;
import squote.service.StockPerformanceService;
import squote.service.WebParserRestService;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static squote.service.MarketReportService.pre;

@RestController
@RequestMapping("/rest/stock")
public class RestStockController {
	private static final String DEFAULT_CODES = "2828";
	public static final String CODE_SEPARATOR = ",";
	private static final int STOCK_QUERY_KEY = 1;

	private static Logger log = LoggerFactory.getLogger(RestStockController.class);

	@Autowired HoldingStockRepository holdingStockRepository;
	@Autowired StockPerformanceService stockPerformanceService;
	@Autowired MarketReportService marketReportService;
	@Autowired WebParserRestService webParserService;
	@Autowired StockQueryRepository stockQueryRepo;
	@Autowired HoldingStockRepository holdingStockRepo;
	@Autowired FundRepository fundRepo;

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

	@RequestMapping(method = RequestMethod.GET, value = "/indexquotes")
	public StockQuote[] listIndexQuotes() throws ExecutionException, InterruptedException {
		return webParserService.getIndexQuotes().get().getBody();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/save/query")
	public String saveQuery(@RequestParam(value="codes", required=false, defaultValue="") String codes) {
		log.info("save codes {} to key {}", codes, STOCK_QUERY_KEY);

		StockQuery q = Optional.ofNullable(stockQueryRepo.findByKey(STOCK_QUERY_KEY))
				.orElse(new StockQuery(codes));
		q.setDelimitedStocks(codes);
		q.setKey(STOCK_QUERY_KEY);
		stockQueryRepo.save(q);
		return stockQueryRepo.findByKey(STOCK_QUERY_KEY).getDelimitedStocks();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/load/query")
	public String loadQuery() {
		log.info("save codes from key {}", STOCK_QUERY_KEY);

		return Optional.ofNullable(stockQueryRepo.findByKey(STOCK_QUERY_KEY))
				.map(StockQuery::getDelimitedStocks)
				.orElse("");
	}

	@RequestMapping("/fullquote")
	public Map<String, Object> quote(@RequestParam(value="codes", required=false, defaultValue="") String codes) throws ExecutionException, InterruptedException {
		log.info("quote: codes [{}]", codes);
		Map<String, Object> resultMap = new HashMap<>();
		codes = retrieveCodes(codes);

		List<HoldingStock> holdingStocks = Lists.newArrayList(holdingStockRepo.findAll(new Sort("date")));
		List<Fund> funds = Lists.newArrayList(fundRepo.findAll());
		Set<String> codeSet = uniqueStockCodes(codes, holdingStocks, funds);
		Future<HttpResponse<StockQuote[]>> stockQuotesFuture = webParserService.getRealTimeQuotes(codeSet);

		// After all concurrent jobs submitted
		Map<String, StockQuote> allQuotes = collectAllStockQuotes(stockQuotesFuture.get().getBody());
		Future<HttpResponse<StockQuote[]>> indexFutures = webParserService.getIndexQuotes();
		List<StockQuote> indexes = Arrays.asList(indexFutures.get().getBody());
		funds.forEach( f -> f.calculateNetProfit(allQuotes) );
		holdingStocks.sort(Comparator.comparing(HoldingStock::getDate));

		resultMap.put("codes", codes);
		resultMap.put("quotes",
				Arrays.stream(codes.split(CODE_SEPARATOR))
						.map(code->allQuotes.get(code))
						.collect(Collectors.toList())
		);
		resultMap.put("indexes", indexes);
		resultMap.put("holdingMap", collectHoldingStockWithQuotesAsMap(holdingStocks, allQuotes));
		resultMap.put("holdings", holdingStocks);
		resultMap.put("allQuotes", allQuotes);
		resultMap.put("funds", funds);
		return resultMap;
	}

	Map<String, StockQuote> collectAllStockQuotes(
			StockQuote[] quotes) {
		return Arrays.stream(quotes)
				.collect(Collectors.toMap(
						StockQuote::getStockCode,
						quote->quote,
						(quote1, quote2) -> {
							log.warn("duplicate quote found: {}", quote2);
							return quote1;
						}
				));
	}

	private Map<HoldingStock, StockQuote> collectHoldingStockWithQuotesAsMap(
			List<HoldingStock> holdingStocks, Map<String, StockQuote> allQuotes) {
		Map<HoldingStock, StockQuote> resultMap = new LinkedHashMap<>();
		holdingStocks.stream()
				.sorted(Comparator.comparing(HoldingStock::getDate))
				.forEach( s -> resultMap.put(s, allQuotes.get(s.getCode())) );

		return resultMap;
	}

	private Set<String> uniqueStockCodes(String codes, List<HoldingStock> holdingStocks, List<Fund> funds) {
		Set<String> codeSet = Sets.newHashSet(codes.split(CODE_SEPARATOR));
		holdingStocks.forEach(x->codeSet.add(x.getCode()));
		funds.forEach( f -> codeSet.addAll(f.getHoldings().keySet()) );
		return codeSet;
	}

	private String retrieveCodes(String reqCodes) {
		if (StringUtils.isNotBlank(reqCodes))
			return reqCodes;
		else
			return DEFAULT_CODES;	// use default codes if still blank
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