package squote.controller;
 
import com.google.common.base.Stopwatch;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.ModelMap;
import squote.SpringQuoteWebApplication;
import squote.SquoteConstants.IndexCode;
import squote.domain.MarketDailyReport;
import squote.domain.StockQuote;
import squote.domain.repository.HoldingStockRepository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static squote.SquoteConstants.NA;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = SpringQuoteWebApplication.class)
@ActiveProfiles("dev")
public class QuoteControllerIntegrationTest {
	private Logger log = LoggerFactory.getLogger(QuoteControllerIntegrationTest.class);

	@Autowired QuoteController quoteController;
	@Autowired HoldingStockRepository holdingStockRepo;
	
	private MockMvc mockMvc;
	   
    @Before
    public void setup() {    	
    	this.mockMvc = MockMvcBuilders.standaloneSetup(quoteController).build();        
    }
    
    @After
    public void clearup() {
    	holdingStockRepo.deleteAll();
    }
    
    @Test
	public void getSingleQuote_Given2800_ShouldReturnXmlMessageWithPrice() throws Exception {
		Stopwatch timer = Stopwatch.createStarted();

		mockMvc.perform(get("/quote/single/2800").characterEncoding(WebConstants.RESPONSE_ENCODING))
			.andExpect(status().isOk())
			.andExpect(content().contentType("application/xml"))
			.andExpect(xpath("/stockQuote[price=NA]").doesNotExist());

		log.debug("getSingleQuote_Given2800_ShouldReturnXmlMessageWithPrice took: {}", timer.stop());
	}	
	
	@SuppressWarnings("unchecked")
	@Test
	public void list_GivenStockCodes_ShouldReturnQuotesWithStocksAndIndexesAndMarketDailyReportHistories() throws Exception {
		// Given
		final String inputCodes = "753,2828,2800,3046,2822,1138,1088,883,2883,489,2333,916,2318,5";

		Stopwatch timer = Stopwatch.createStarted();

		MvcResult mvcResult = mockMvc.perform(get("/quote/list?codes=" + inputCodes).characterEncoding(WebConstants.RESPONSE_ENCODING))
		.andExpect(status().isOk())
		.andExpect(view().name("quote/list")).andReturn();

		log.debug("quote list page took: {}", timer.stop());
		
		ModelMap modelMap = mvcResult.getModelAndView().getModelMap();
		List<StockQuote> indexes = (List<StockQuote>) modelMap.get("indexes");
		List<StockQuote> quotes = (List<StockQuote>) modelMap.get("quotes");
		StockQuote hscei = (StockQuote) modelMap.get("hsce");
				
		assertTrue(modelMap.get("codes").equals(inputCodes));
		assertNotNull(modelMap.get("tbase"));

		Map<String, MarketDailyReport> tHistory = (Map<String, MarketDailyReport>) modelMap.get("tHistory");
		marketDailyReportContainIndexes(tHistory.get("T-1"));
		marketDailyReportContainIndexes(tHistory.get("T-7"));
		marketDailyReportContainIndexes(tHistory.get("T-30"));
		assertTrue(tHistory.containsKey("T-60"));
		assertNotNull(tHistory.get("T-60"));
				
		StockQuote quote1 = quotes.get(0);
		StockQuote quote2 = quotes.get(1);
		assertTrue(!"NA".equals(quote1.getPrice()));
		assertEquals("753",quote1.getStockCode());
		assertTrue(!"NA".equals(quote2.getPrice()));
		assertEquals("2828",quote2.getStockCode());
				
		assertEquals(IndexCode.HSCEI.toString(), hscei.getStockCode());
		
		assertNotNull(modelMap.get("funds"));
		
		assertEquals(6, indexes.size());
	}

	private void marketDailyReportContainIndexes(MarketDailyReport report) {
		assertTrue(report.getMoneyBase().getTotal() > 0);
		assertNotEquals(NA, report.getHsi().getStockCode());
		assertNotEquals(NA, report.getHscei().getStockCode());
	}

	@Test
	public void listStocksPerformance_ShouldReturnOne2828QuoteAndAllQuoteSortedByLastYearPercentageChg() throws ExecutionException, InterruptedException {
		Stopwatch timer = Stopwatch.createStarted();

		List<StockQuote> quotes = quoteController.listStocksPerformance();
		assertTrue(quotes.size() > 50);

		StockQuote quotes2828 = quotes.stream().filter(q -> "2828".equals(q.getStockCode())).findFirst().get();
		assertEquals("2828", quotes2828.getStockCode());
		assertNotEquals(0.0, quotes2828.getLastYearPercentage());
		assertNotEquals(0.0, quotes2828.getLast2YearPercentage());
		assertNotEquals(0.0, quotes2828.getLast3YearPercentage());

		for (int i=0; i < quotes.size()-1; i++) {
			assertTrue("Quotes are sort by last year percentage" , Double.compare(quotes.get(i).getLastYearPercentage(), quotes.get(i+1).getLastYearPercentage()) <=0);
		}

		log.debug("getStockPerformanceQuotes_ShouldReturnOne2828QuoteAndAllQuoteSortedByLastYearPercentageChg took: {}", timer.stop());
	}

	@Test
	public void listStocksPerformance_givenDoubleCall_shouldReturnSameObj() throws ExecutionException, InterruptedException {
		List<StockQuote> quotes = quoteController.listStocksPerformance();
		assertEquals(quotes, quoteController.listStocksPerformance());
	}
}
