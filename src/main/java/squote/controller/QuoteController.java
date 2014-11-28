package squote.controller;

import static squote.SquoteConstants.IndexCode.HSCEI;
import static squote.service.MarketReportService.pre;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import squote.SquoteConstants.IndexCode;
import squote.domain.HoldingStock;
import squote.domain.MarketDailyReport;
import squote.domain.StockExecutionMessage;
import squote.domain.StockQuery;
import squote.domain.StockQuote;
import squote.domain.repository.HoldingStockRepository;
import squote.domain.repository.StockQueryRepository;
import squote.service.CentralWebQueryService;
import squote.service.MarketReportService;
import squote.service.StockPerformanceService;
import squote.web.parser.AastockStockQuoteParser;
import squote.web.parser.EtnetIndexQuoteParser;
import squote.web.parser.HSINetParser;
import thc.util.ConcurrentUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

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
	
	public QuoteController() {
		super("quote");
	}
	
	@RequestMapping(value = "/single/{code}", produces="application/xml")	
	public @ResponseBody StockQuote single(@PathVariable String code) {
		log.debug("single: reqCode [{}]", code);		
		return StringUtils.isBlank(code)? new StockQuote() : new AastockStockQuoteParser(code).getStockQuote();				
	}
	
	@RequestMapping(value="/createholdingstock")
	public String createHoldingStockFromExecution(@RequestParam(value="message", required=false, defaultValue="") String message,
			@RequestParam(value="hscei", required=false, defaultValue="0") String hscei,
			ModelMap modelMap) {
		
		log.debug("createholdingstock: message[{}]", message);
		if (StringUtils.isBlank(message)) return page("/createholdingstock");
		
		String resultMessage = "";
		HoldingStock holdingStock = null;
		
		Optional<StockExecutionMessage> executionMessage = StockExecutionMessage.construct(message);
		if (!executionMessage.isPresent()) 
			resultMessage = "Cannot create holding stock";
		else {
			hscei = enrichHscei(hscei, executionMessage.get().getDate());									
			if ("0".equals(hscei)) {
				resultMessage = "Cannot get hscei";
			} else {
				holdingStock = createAndSaveHoldingStocks(executionMessage.get(), new BigDecimal(hscei));
				resultMessage = "Created holding stock";
			}
		}
		
		modelMap.put("holdingStock", holdingStock);
		modelMap.put("resultMessage", resultMessage);		
		return page("/createholdingstock");
	}
	
	private String enrichHscei(String hscei, Date date) {
		if ("0".equals(hscei)) {
			try {
				return parseHsceiFromWeb(date);
			} catch (Exception e) {
				return "0";
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
	
	@RequestMapping(value="/list", method = RequestMethod.GET)
	public String list(@RequestParam(value="codes", required=false, defaultValue="") String reqCodes,
			@RequestParam(value="action", required=false, defaultValue="") String action,
			@CookieValue(value=CODES_COOKIE_KEY, required=false) String cookieCodes,
			HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) {
		
		log.debug("list: reqCodes [{}], action [{}]", reqCodes, action);
			
		String codes = retrieveCodes(reqCodes, action, cookieCodes);	
		saveQueryIfNeeded(codes, action);		
		updateCookie(codes, response);
			
		List<HoldingStock> holdingStocks = Lists.newArrayList(holdingStockRepo.findAll(new Sort("date")));
		Set<String> codeSet = uniqueStockCodes(codes, holdingStocks); 
				
		// Submit web queries
		Future<Optional<List<StockQuote>>> indexeFutures = webQueryService.submit(new EtnetIndexQuoteParser());
		List<CompletableFuture<StockQuote>> stockQuoteFutures = submitStockQuoteRequests(codeSet);		
		List<Future<MarketDailyReport>> mktReports = submitMarketDailyReportRequests();
	
		// After all concurrent jobs submitted
		List<StockQuote> indexes = ConcurrentUtils.collect(indexeFutures).get();
		Map<String, StockQuote> allQuotes = collectAllStockQuotes(stockQuoteFutures);
			
		modelMap.put("codes", codes);
		modelMap.put("quotes", 
				Arrays.stream(codes.split(CODE_SEPARATOR))
					.map(code->allQuotes.get(code))
					.iterator()				
				);
		modelMap.put("indexes", indexes);
		modelMap.put("tbase", ConcurrentUtils.collect(mktReports.get(0)));
		modelMap.put("tminus1", ConcurrentUtils.collect(mktReports.get(1)));
		modelMap.put("tminus7", ConcurrentUtils.collect(mktReports.get(2)));
		modelMap.put("tminus30", ConcurrentUtils.collect(mktReports.get(3)));
		modelMap.put("tminus60", ConcurrentUtils.collect(mktReports.get(4)));
		modelMap.put("holdingMap", holdingStocks.stream().collect(Collectors.toMap(x->x, x->allQuotes.get(x.getCode()))));
		modelMap.put("hsce", indexes.stream().filter(a -> IndexCode.HSCEI.name.equals(a.getStockCode())).findFirst().get());
		
		return page("/list");
	}

	private List<Future<MarketDailyReport>> submitMarketDailyReportRequests() {
		return mktReportService.getMarketDailyReport(
				pre(1, Calendar.DATE),
				pre(2, Calendar.DATE),
				pre(7, Calendar.DATE),
				pre(30, Calendar.DATE),
				pre(60, Calendar.DATE)
			);
	}

	private Map<String, StockQuote> collectAllStockQuotes(
			List<CompletableFuture<StockQuote>> stockQuoteFutures) {
		return stockQuoteFutures.stream()
				.map(f -> f.join())
				.collect(Collectors.toMap(StockQuote::getStockCode, quote->quote));
	}

	private List<CompletableFuture<StockQuote>> submitStockQuoteRequests(Set<String> codeSet) {
		return codeSet.stream()
				.map( code -> 
					webQueryService.submit(() -> new AastockStockQuoteParser(code).getStockQuote()) 
				)
				.collect(Collectors.toList());
	}
	
	private Set<String> uniqueStockCodes(String codes, List<HoldingStock> holdingStocks) {
		Set<String> codeSet = Sets.newHashSet(codes.split(CODE_SEPARATOR));		
		holdingStocks.forEach(x->codeSet.add(x.getCode()));
		return codeSet;
	}

	@RequestMapping(value = "/stocksperf")	
	public String stocksPerfPage() {return page("/stocksperf");}
	
	@RequestMapping(method = RequestMethod.GET, value = "/liststocksperf")	
	public @ResponseBody Map<String, Object> listStocksPerformance() {		
		return stockPerformanceService.getStockPerformanceMap();
	}
	
	private HoldingStock createAndSaveHoldingStocks(StockExecutionMessage message, BigDecimal hscei) {
		HoldingStock s = new HoldingStock(
							message.getCode(), 
							message.getSide(), 
							message.getQuantity(), 
							new BigDecimal(message.getPrice() * message.getQuantity()), 
							message.getDate(), 
							hscei);
		
		holdingStockRepo.save(s);
		return s;
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
				StockQuery q = stockQueryRepo.findByKey(STOCK_QUERY_KEY);
				if (q == null) {
					q = new StockQuery(codes);
					q.setKey(STOCK_QUERY_KEY);
				}
				else 
					q.setDelimitedStocks(codes);		
				
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