package squote.controller;
 
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import squote.SquoteConstants;
import squote.SquoteConstants.IndexCode;
import squote.domain.HoldingStock;
import squote.domain.MarketDailyReport;
import squote.domain.StockQuote;
import squote.domain.repository.HoldingStockRepository;
import thc.util.DateUtils;
 
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = SpringQuoteWebApplication.class)
@ActiveProfiles("dev")
public class QuoteControllerIntegrationTest {
	@Autowired QuoteController quoteController;
	@Autowired HoldingStockRepository holdingStockRepo;
	
	private MockMvc mockMvc;
	   
    @Before
    public void setup() {    	
    	this.mockMvc = MockMvcBuilders.standaloneSetup(quoteController).build();        
    }
    
    @AfterClass
    public static void clearup() {
    	holdingStockRepo.deleteAll();
    }
    
    @Test
	public void getSingleQuote_Given2800_ShouldReturnXmlMessageWithPrice() throws Exception {	
		mockMvc.perform(get("/quote/single/2800").characterEncoding(WebConstants.RESPONSE_ENCODING))
			.andExpect(status().isOk())
			.andExpect(content().contentType("application/xml"))
			.andExpect(xpath("/stockQuote[price=NA]").doesNotExist());
	}	
	
	@SuppressWarnings("unchecked")
	@Test
	public void list_GivenStockCodes_ShouldReturnQuotesWithStocksAndIndexesAndMarketDailyReportHistories() throws Exception {
		// Given
		final String inputCodes = "753,2828,2800";
		        
		MvcResult mvcResult = mockMvc.perform(get("/quote/list?codes=" + inputCodes).characterEncoding(WebConstants.RESPONSE_ENCODING))
		.andExpect(status().isOk())
		.andExpect(view().name("quote/list")).andReturn();
		
		ModelMap modelMap = mvcResult.getModelAndView().getModelMap();
		List<StockQuote> indexes = (List<StockQuote>) modelMap.get("indexes");
		List<StockQuote> quotes = (List<StockQuote>) modelMap.get("quotes");
		StockQuote hscei = (StockQuote) modelMap.get("hsce");
				
		assertTrue(modelMap.get("codes").equals(inputCodes));
		assertNotNull(modelMap.get("tbase"));
		Map<String, MarketDailyReport> tHistory = (Map<String, MarketDailyReport>) modelMap.get("tHistory");
		assertTrue(tHistory.containsKey("T-1"));
		assertTrue(tHistory.containsKey("T-7"));
		assertTrue(tHistory.containsKey("T-30"));
		assertTrue(tHistory.containsKey("T-60"));
		assertNotNull(tHistory.get("T-1"));
		assertNotNull(tHistory.get("T-7"));
		assertNotNull(tHistory.get("T-30"));
		assertNotNull(tHistory.get("T-60"));
				
		StockQuote quote1 = quotes.get(0);
		StockQuote quote2 = quotes.get(1);
		assertTrue(!"NA".equals(quote1.getPrice()));
		assertEquals("753",quote1.getStockCode());
		assertTrue(!"NA".equals(quote2.getPrice()));
		assertEquals("2828",quote2.getStockCode());
				
		assertEquals(IndexCode.HSCEI.name, hscei.getStockCode());
		
		assertNotNull(modelMap.get("funds"));
		
		assertEquals(2, indexes.size());
	}
	
	@Test	
	public void postCreateHoldingStock_GivenTodayExeMsg_ShouldCreateHoldingStockUseIndexFromRealtimeQuote() throws Exception {
		String scbSellMsg = "渣打: (沽出10,000股01138.中海發展股份) \n";
		scbSellMsg += "已於4.8900元成功執行\n";
		scbSellMsg += DateUtils.toString(new Date(), "yyyyMMdd") + "000013235";
		
		long total = holdingStockRepo.count();
				
		// When
		MvcResult mvcResult = mockMvc.perform(
					post("/quote/createholdingstock").characterEncoding("utf-8")
					.param("message", scbSellMsg)
				).andExpect(status().isOk())
				.andExpect(model().attribute("resultMessage", "Created holding stock"))
				.andExpect(view().name("quote/createholdingstock")).andReturn();
		
		// Expect
		HoldingStock holdingStock = (HoldingStock) mvcResult.getModelAndView().getModelMap().get("holdingStock");
		assertNotNull(holdingStock);
		assertEquals("1138", holdingStock.getCode());
		assertEquals(10000, holdingStock.getQuantity());
		assertEquals(new BigDecimal("48900.0000"), holdingStock.getGross());
		assertEquals(SquoteConstants.Side.SELL, holdingStock.getSide());
		assertEquals(total+1, holdingStockRepo.count());
	}
}
