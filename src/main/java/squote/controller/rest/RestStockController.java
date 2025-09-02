package squote.controller.rest;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mashape.unirest.http.HttpResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import squote.SquoteConstants;
import squote.domain.*;
import squote.domain.repository.DailyAssetSummaryRepository;
import squote.domain.repository.FundRepository;
import squote.domain.repository.HoldingStockRepository;
import squote.domain.repository.StockQueryRepository;
import squote.scheduletask.StockTradingTask;
import squote.security.AuthenticationService;
import squote.service.*;

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

	private static Logger log = LoggerFactory.getLogger(RestStockController.class);

	@Autowired StockPerformanceService stockPerformanceService;
	@Autowired MarketReportService marketReportService;
	@Autowired WebParserRestService webParserService;
	@Autowired StockQueryRepository stockQueryRepo;
	@Autowired HoldingStockRepository holdingStockRepo;
	@Autowired FundRepository fundRepo;
	@Autowired AuthenticationService authenticationService;
	@Autowired BinanceAPIService binanceAPIService;
	@Autowired DailyAssetSummaryRepository dailyAssetSummaryRepository;
	@Autowired StockTradingTask stockTradingTask;
	@Autowired YahooFinanceService yahooFinanceService;

	@RequestMapping("/holding/list")
	public Iterable<HoldingStock> listHolding() {
		log.info("list holding");
		return holdingStockRepo.findByUserIdOrderByDate(authenticationService.getUserId().get());
	}
	
	@RequestMapping("/holding/delete/{id}")
	public Iterable<HoldingStock> deleteHolding(@PathVariable String id) {
		log.warn("delete: id [{}]", id);
		holdingStockRepo.deleteById(id);
		return holdingStockRepo.findByUserIdOrderByDate(authenticationService.getUserId().get());
	}

	@DeleteMapping("/holding/delete-pair/{id1}/{id2}")
	public Iterable<HoldingStock> deleteHoldingPair(@PathVariable String id1, @PathVariable String id2) {
		log.warn("Deleting buy/sell pair: {}, {}", id1, id2);

		var holding1Optional = holdingStockRepo.findById(id1);
		var holding2Optional = holdingStockRepo.findById(id2);
		if (holding1Optional.isPresent() && holding2Optional.isPresent()) {
			var holding1 = holding1Optional.get();
			var holding2 = holding2Optional.get();
			var earning = holding1.getGross().subtract(holding2.getGross()).abs();
			if (holding1.getFee() != null) earning = earning.subtract(holding1.getFee());
			if (holding2.getFee() != null) earning = earning.subtract(holding2.getFee());
			log.info("{}({}) earn {}", holding1.getUserId(), holding1.getFundName(), earning);
		}

		holdingStockRepo.deleteById(id1);
		holdingStockRepo.deleteById(id2);

		return holdingStockRepo.findByUserIdOrderByDate(authenticationService.getUserId().get());
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
		Optional<String> userId = authenticationService.getUserId();
		log.info("save codes {} to userId {}", codes, userId);

		StockQuery q = stockQueryRepo.findByUserId(userId.get())
						.orElse(new StockQuery(userId.get(), codes));
		q.setDelimitedStocks(codes);
		stockQueryRepo.save(q);
		return stockQueryRepo.findByUserId(userId.get()).get().getDelimitedStocks();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/load/query")
	public String loadQuery() {
		Optional<String> userId = authenticationService.getUserId();
		log.info("load codes from userId {}", userId);

		return userId.flatMap(v -> stockQueryRepo.findByUserId(v))
				.flatMap(q -> Optional.ofNullable(q.getDelimitedStocks()))
				.orElse("");
	}

	@RequestMapping("/fullquote")
	public Map<String, Object> quote(@RequestParam(value="codes", required=false, defaultValue="") String codes) throws ExecutionException, InterruptedException {
		String userId = authenticationService.getUserId().get();
		log.info("quote: codes [{}] for userId [{}]", codes, userId);
		Map<String, Object> resultMap = new HashMap<>();
		codes = retrieveCodes(codes);

		List<HoldingStock> holdingStocks = Lists.newArrayList(holdingStockRepo.findByUserIdOrderByDate(userId));
		List<Fund> funds = fundRepo.findByUserId(userId);
		Set<String> codeSet = uniqueStockCodes(codes, holdingStocks, funds);
		Map<Market, Set<String>> separatedCodes = separateStockCodesByMarket(codeSet);
		Future<HttpResponse<StockQuote[]>> hkStockQuotesFuture = separatedCodes.get(Market.HK).isEmpty() ? null : webParserService.getRealTimeQuotes(separatedCodes.get(Market.HK));
		Set<String> USStockCodes = separatedCodes.get(Market.US);
		yahooFinanceService.subscribeToSymbols(USStockCodes.toArray(new String[0]));

		// After all concurrent jobs submitted
		Map<String, StockQuote> allQuotes = hkStockQuotesFuture == null ? new HashMap<>() : collectAllStockQuotes(hkStockQuotesFuture.get().getBody());
		allQuotes.putAll(binanceAPIService.getAllPrices());
		allQuotes.putAll(getUSStockQuotes(USStockCodes));
		Future<HttpResponse<StockQuote[]>> indexFutures = webParserService.getIndexQuotes();
		List<StockQuote> indexes = Arrays.asList(indexFutures.get().getBody());
		funds.forEach( f -> f.calculateNetProfit(allQuotes) );
		holdingStocks.sort(Comparator.comparing(HoldingStock::getDate));

		resultMap.put("codes", codes);
		resultMap.put("quotes",
				Arrays.stream(codes.split(CODE_SEPARATOR))
						.filter(allQuotes::containsKey)
						.map(allQuotes::get)
						.collect(Collectors.toList())
		);
		resultMap.put("indexes", indexes);
		resultMap.put("holdingMap", collectHoldingStockWithQuotesAsMap(holdingStocks, allQuotes));
		resultMap.put("holdings", holdingStocks);
		resultMap.put("allQuotes", allQuotes);
		resultMap.put("funds", funds);
		return resultMap;
	}

	private Map<String, ? extends StockQuote> getUSStockQuotes(Set<String> usStockCodes) {
		if (usStockCodes.isEmpty()) return Collections.emptyMap();

		return usStockCodes.stream()
				.flatMap(code -> yahooFinanceService.getLatestTicker(code).stream())
				.collect(Collectors.toMap(
						StockQuote::getStockCode,
						quote -> quote,
						(existing, replacement) -> existing)
				);
	}

	@GetMapping("/summary/latest")
	public ResponseEntity<Map<String, DailyAssetSummary>> getLatestSummaries(@RequestParam List<String> symbols) {
		Map<String, DailyAssetSummary> summaryMap = new HashMap<>();
		for (String symbol : symbols) {
			Optional<DailyAssetSummary> summary = dailyAssetSummaryRepository.findTopBySymbolOrderByDateDesc(symbol);
			summary.ifPresent(s -> summaryMap.put(symbol, s));
		}

		return ResponseEntity.ok(summaryMap);
	}

	@GetMapping("/trading/enabledByMarket")
	public Map<String, Boolean> getStockTradingTaskEnabledByMarket() {
		return stockTradingTask.enabledByMarket;
	}

	@PostMapping("/trading/enable/{market}/{value}")
	public void setStockTradingTaskEnable(@PathVariable String market, @PathVariable boolean value) {
		var newMap = new HashMap<>(stockTradingTask.enabledByMarket);
		newMap.put(market, value);
		stockTradingTask.enabledByMarket = newMap;
	}

	Map<String, StockQuote> collectAllStockQuotes(StockQuote[] quotes) {
		return Arrays.stream(quotes)
				.filter(s -> !SquoteConstants.NA.equals(s.getStockCode()))
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
		funds.stream()
				.filter(f -> Fund.FundType.STOCK == f.getType())
				.forEach(f -> codeSet.addAll(f.getHoldings().keySet()) );
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

	private Map<Market, Set<String>> separateStockCodesByMarket(Set<String> codeSet) {
		var result = new HashMap<Market, Set<String>>();
		result.put(Market.US, new HashSet<>());
		result.put(Market.HK, new HashSet<>());

		for (String code : codeSet) {
			if (Market.isUSStockCode(code)) {
				result.get(Market.US).add(code);
			} else {
				result.get(Market.HK).add(code);
			}
		}

		return result;
	}
}
