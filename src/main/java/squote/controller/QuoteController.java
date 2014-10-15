package squote.controller;

import static squote.service.MarketReportService.pre;
import static squote.web.parser.HSINetParser.IndexCode.HSCEI;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import squote.domain.HoldingStock;
import squote.domain.MarketDailyReport;
import squote.domain.StockExecutionMessage;
import squote.domain.StockQuery;
import squote.domain.StockQuote;
import squote.domain.repository.StockQueryRepository;
import squote.service.CentralWebQueryService;
import squote.service.MarketReportService;
import squote.service.QuoteService;
import squote.service.StockPerformanceService;
import squote.web.parser.AastockStockQuoteParser;
import squote.web.parser.HSINetParser;
import thc.util.ConcurrentUtils;
import thc.util.HttpClient;

import com.google.common.base.Optional;

@RequestMapping("/quote")
@Controller
public class QuoteController extends AbstractController {
	private static Logger log = LoggerFactory.getLogger(QuoteController.class);
	private static final String DEFAULT_CODE_LIST = "2828";
	private static final String CODELIST_COOKIE_KEY = "codeList";
	private static final int STOCK_QUERY_KEY = 1;	
	
	@Autowired StockQueryRepository stockQueryRepo;
	@Autowired QuoteService quoteService;
	@Autowired MarketReportService mktReportService;
	@Autowired StockPerformanceService stockPerformanceService;
	@Autowired CentralWebQueryService webQueryService;
	
	public QuoteController() {
		super("quote");
	}
	
	@RequestMapping(value = "/single/{code}")	
	public @ResponseBody StockQuote single(@PathVariable String code) {
		log.debug("single: reqCode [{}]", code);
		
		if (StringUtils.isBlank(code)) {			
			return new StockQuote();
		}
		
		return new AastockStockQuoteParser(code).getStockQuote();
	}
	
	@RequestMapping(value="/createholdingstock")
	public String createHoldingStockFromExecution(@RequestParam(value="message", required=false, defaultValue="") String message,
			@RequestParam(value="hscei", required=false, defaultValue="0") String hscei,
			ModelMap modelMap) {
		log.debug("createholdingstock: message[{}]", message);

		String resultMessage = "";
	
		Optional<StockExecutionMessage> executionMessage = StockExecutionMessage.construct(message);
		if (!executionMessage.isPresent()) resultMessage = "Cannot create holding stock";
		else {
			if ("0".equals(hscei)) {
				Optional<StockQuote> indexQuote = webQueryService.parse(new HSINetParser(HSCEI, executionMessage.get().getDate()));
				if (indexQuote.isPresent()) {
					hscei = indexQuote.get().getPrice();
				} else {
					resultMessage = "Cannot get hscei";
				}
			}
			if (!"0".equals(hscei)) {
				HoldingStock holdingStock = quoteService.createAndSaveHoldingStocks(executionMessage.get(), new BigDecimal(hscei));
				resultMessage = "Created holding stock";
				modelMap.put("holdingStock", holdingStock);
			}
		}
		
		modelMap.put("resultMessage", resultMessage);
		
		return page("/createholdingstock");
	}
	
	@RequestMapping(value="/list", method = RequestMethod.GET)
	public String list(@RequestParam(value="codeList", required=false, defaultValue="") String codeList,
			@RequestParam(value="action", required=false, defaultValue="") String action,
			@CookieValue(value=CODELIST_COOKIE_KEY, required=false) String cookieCodeList,
			HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) {
		
		log.debug("list: reqCodeList [${codeList}], action [${persistAction}]");
			
		codeList = retrieveCodeList(codeList, action, cookieCodeList);	
		saveQueryIfNeeded(codeList, action);		
		updateCookie(codeList, response);
	
		List<Future<StockQuote>> quotesFutures = quoteService.getStockQuoteList(codeList);
		Future<List<StockQuote>> indexeFutures = quoteService.getGlobalIndexes();
		Future<Map<HoldingStock, StockQuote>> holdingStocksFuture = quoteService.getAllHoldingStocksWithMapping();
		List<Future<MarketDailyReport>> mktReports = mktReportService.getMarketDailyReport(
				pre(1, Calendar.DATE),
				pre(2, Calendar.DATE),
				pre(7, Calendar.DATE),
				pre(30, Calendar.DATE),
				pre(60, Calendar.DATE)
			);
	
		// After all concurrent jobs submitted
		List<StockQuote> indexes = ConcurrentUtils.collect(indexeFutures);
		
		modelMap.put("codeList", codeList);
		modelMap.put("quotes", ConcurrentUtils.collects(quotesFutures));
		modelMap.put("indexes", indexes);
		modelMap.put("tbase", ConcurrentUtils.collect(mktReports.get(0)));
		modelMap.put("tminus1", ConcurrentUtils.collect(mktReports.get(1)));
		modelMap.put("tminus7", ConcurrentUtils.collect(mktReports.get(2)));
		modelMap.put("tminus30", ConcurrentUtils.collect(mktReports.get(3)));
		modelMap.put("tminus60", ConcurrentUtils.collect(mktReports.get(4)));
		modelMap.put("holdingMap", ConcurrentUtils.collect(holdingStocksFuture));
		modelMap.put("hsce", indexes.get(1));
		
		return page("/list");
	}
	
	@RequestMapping(value = "/stocksperf")	
	public String stocksPerfPage() {return page("/stocksperf");}
	
	@RequestMapping(method = RequestMethod.GET, value = "/liststocksperf")	
	public @ResponseBody Map<String, Object> listStocksPerformance() {		
		return stockPerformanceService.getStockPerformanceMap();
	}
	
	// A schedule job to refresh context
	public void refreshContext() {
		log.debug(new HttpClient().makeGetRequest("http://rooquote.herokuapp.com/").toString());
	}

	private void updateCookie(String codeList, HttpServletResponse response) {
		Cookie c = new Cookie(CODELIST_COOKIE_KEY,codeList);
		c.setMaxAge(60*60*24*365);
		response.addCookie(c);
	}

	private void saveQueryIfNeeded(String codeList, String action) {
		try {				
			if ("save".equals(action.toLowerCase())) {
				StockQuery q = stockQueryRepo.findByKey(STOCK_QUERY_KEY);
				if (q == null) {
					q = new StockQuery(codeList);
					q.setKey(STOCK_QUERY_KEY);
				}
				else 
					q.setStockList(codeList);														
				stockQueryRepo.save(q);
			}
		} catch (Exception e) {
			log.error("Save StockQuery failed:", e);
		}
	}

	private String retrieveCodeList(String codeList, String action, String cookieCodeList) {
		if ("load".equals(action.toLowerCase())) { 
			StockQuery q = stockQueryRepo.findByKey(STOCK_QUERY_KEY);
			if (q != null) return q.getStockList();
		}
		
		if (StringUtils.isNotBlank(codeList)) return codeList; // use cookie code list if input is blank
		if (StringUtils.isNotBlank(cookieCodeList)) return cookieCodeList;						
		return codeList = DEFAULT_CODE_LIST;	// use default list if still blank
	}
}