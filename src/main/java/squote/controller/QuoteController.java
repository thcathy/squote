package squote.controller;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mashape.unirest.http.HttpResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import squote.SquoteConstants.IndexCode;
import squote.domain.*;
import squote.domain.repository.FundRepository;
import squote.domain.repository.HoldingStockRepository;
import squote.domain.repository.StockQueryRepository;
import squote.service.CentralWebQueryService;
import squote.service.MarketReportService;
import squote.service.StockPerformanceService;
import squote.service.WebParserRestService;
import thc.util.ConcurrentUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static squote.service.MarketReportService.pre;

@RequestMapping("/quote")
@Controller
public class QuoteController extends AbstractController {
	private static Logger log = LoggerFactory.getLogger(QuoteController.class);
	private static final String DEFAULT_CODES = "2828";
	private static final String CODES_COOKIE_KEY = "codes";
	private static final int STOCK_QUERY_KEY = 1;	
	public static final String CODE_SEPARATOR = ",";
	
	@Autowired StockQueryRepository stockQueryRepo;	
	@Autowired MarketReportService mktReportService;
	@Autowired StockPerformanceService stockPerformanceService;
	@Autowired CentralWebQueryService webQueryService;
	@Autowired HoldingStockRepository holdingStockRepo;
	@Autowired FundRepository fundRepo;
	@Autowired WebParserRestService webParserService;
	
	public QuoteController() {
		super("quote");
	}
	
	@RequestMapping(value = "/single/{code}")
	public @ResponseBody StockQuote single(@PathVariable String code) throws Exception {
		log.debug("single: reqCode [{}]", code);
		return webParserService.getFullQuote("2").get().getBody();
	}

	@RequestMapping(value="/list", method = RequestMethod.GET)
	public String list(@RequestParam(value="codes", required=false, defaultValue="") String reqCodes,
			@RequestParam(value="action", required=false, defaultValue="") String action,
			@CookieValue(value=CODES_COOKIE_KEY, required=false) String cookieCodes,
			HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) throws ExecutionException, InterruptedException {
		
		log.debug("list: reqCodes [{}], action [{}]", reqCodes, action);
			
		String codes = retrieveCodes(reqCodes, action, cookieCodes);	
		saveQueryIfNeeded(codes, action);		
		updateCookie(codes, response);
			
		List<HoldingStock> holdingStocks = Lists.newArrayList(holdingStockRepo.findAll(new Sort("date")));
		List<Fund> funds = Lists.newArrayList(fundRepo.findAll());
		Set<String> codeSet = uniqueStockCodes(codes, holdingStocks, funds);
		Future<HttpResponse<StockQuote[]>> stockQuotesFuture = webParserService.getRealTimeQuotes(codeSet);		
		
		Future<HttpResponse<StockQuote[]>> indexFutures = webParserService.getIndexQuotes();
		List<CompletableFuture<MarketDailyReport>> mktReports = submitMarketDailyReportRequests();
	
		// After all concurrent jobs submitted
		List<StockQuote> indexes = Arrays.asList(indexFutures.get().getBody());
		Map<String, StockQuote> allQuotes = collectAllStockQuotes(stockQuotesFuture.get().getBody());
		funds.forEach( f -> f.calculateNetProfit(allQuotes) );
			
		modelMap.put("codes", codes);
		modelMap.put("quotes",
				Arrays.stream(codes.split(CODE_SEPARATOR))
					.map(code->allQuotes.get(code))
					.collect(Collectors.toList())				
				);
		modelMap.put("indexes", indexes);
		modelMap.put("tbase", ConcurrentUtils.collect(mktReports.get(0)));
		modelMap.put("tHistory", collectMktReportHistories(mktReports));		
		modelMap.put("holdingMap", collectHoldingStockWithQuotesAsMap(holdingStocks, allQuotes));
		modelMap.put("hsce", indexes.stream().filter(a -> IndexCode.HSCEI.toString().equals(a.getStockCode())).findFirst().get());
		modelMap.put("funds", funds);
		
		return page("/list");
	}

	private Map<HoldingStock, StockQuote> collectHoldingStockWithQuotesAsMap(
			List<HoldingStock> holdingStocks, Map<String, StockQuote> allQuotes) {
		Map<HoldingStock, StockQuote> resultMap = new LinkedHashMap<>();
		holdingStocks.stream()
			.sorted( (s1, s2) -> s1.getDate().compareTo(s2.getDate()) )
			.forEach( s -> resultMap.put(s, allQuotes.get(s.getCode())) );
		
		return resultMap;
	}

	private Object collectMktReportHistories(List<CompletableFuture<MarketDailyReport>> mktReports) {
		Map<String, MarketDailyReport> histories = new LinkedHashMap<>();
		histories.put("T-1", mktReports.get(1).join());
		histories.put("T-7", mktReports.get(2).join());
		histories.put("T-30", mktReports.get(3).join());
		histories.put("T-60", mktReports.get(4).join());		
		return histories;
	}

	private List<CompletableFuture<MarketDailyReport>> submitMarketDailyReportRequests() {
		return mktReportService.getMarketDailyReport(
				pre(1, Calendar.DATE),
				pre(2, Calendar.DATE),
				pre(7, Calendar.DATE),
				pre(30, Calendar.DATE),
				pre(60, Calendar.DATE)
			);
	}

	private Map<String, StockQuote> collectAllStockQuotes(
			StockQuote[] quotes) {
		return Arrays.stream(quotes)
				.collect(Collectors.toMap(StockQuote::getStockCode, quote->quote));
	}

	private Set<String> uniqueStockCodes(String codes, List<HoldingStock> holdingStocks, List<Fund> funds) {
		Set<String> codeSet = Sets.newHashSet(codes.split(CODE_SEPARATOR));		
		holdingStocks.forEach(x->codeSet.add(x.getCode()));
		funds.forEach( f -> codeSet.addAll(f.getHoldings().keySet()) );
		return codeSet;
	}

	@RequestMapping(value = "/stocksperf")	
	public String stocksPerfPage() {return page("/stocksperf");}
	
	@RequestMapping(method = RequestMethod.GET, value = "/liststocksperf")	
	public @ResponseBody List<StockQuote> listStocksPerformance() {		
		return stockPerformanceService.getStockPerformanceQuotes();
	}
		
	private HttpServletResponse updateCookie(String codes, HttpServletResponse response) {
		Cookie c = new Cookie(CODES_COOKIE_KEY,codes);
		c.setMaxAge(60*60*24*365);
		response.addCookie(c);
		return response;
	}

	private void saveQueryIfNeeded(String codes, String action) {
		try {				
			if ("save".equals(action.toLowerCase())) {
				StockQuery q = Optional.ofNullable(stockQueryRepo.findByKey(STOCK_QUERY_KEY))
										.orElse(new StockQuery(codes));
					
				q.setDelimitedStocks(codes);
				q.setKey(STOCK_QUERY_KEY);
				stockQueryRepo.save(q);
			}
		} catch (Exception e) {
			log.error("Save StockQuery failed:", e);
		}
	}

	private String retrieveCodes(String reqCodes, String action, String cookieCodes) {
		if ("load".equals(action.toLowerCase())) { 
			StockQuery q = stockQueryRepo.findByKey(STOCK_QUERY_KEY);
			if (q != null) return q.getDelimitedStocks();
		}
		
		if (StringUtils.isNotBlank(reqCodes)) return reqCodes; // use cookie codes if input is blank
		if (StringUtils.isNotBlank(cookieCodes)) return cookieCodes;						
		return DEFAULT_CODES;	// use default codes if still blank
	}
}