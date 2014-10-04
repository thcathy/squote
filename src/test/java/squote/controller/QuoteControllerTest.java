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
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
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
import squote.domain.HoldingStock;
import squote.domain.StockQuote;
import squote.service.CentralWebQueryService;
import squote.web.parser.HSINetParser;

import com.google.common.base.Optional;
 
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = SpringQuoteWebApplication.class)
@ActiveProfiles("dev")
public class QuoteControllerTest {
	@Mock CentralWebQueryService mockWebQueryService;
		
	@Autowired QuoteController quoteController;
	
	 private MockMvc mockMvc;
    
    @Before
    public void setup() {    	
    	MockitoAnnotations.initMocks(this);
    	quoteController.webQueryService = mockWebQueryService;
        this.mockMvc = MockMvcBuilders.standaloneSetup(quoteController).build();
    }
    
    @Test
	public void getSingleQuote() throws Exception {	
		mockMvc.perform(get("/quote/single/2800").characterEncoding("utf-8"))
			.andExpect(status().isOk())
			.andExpect(content().contentType("application/xml"))
			.andExpect(xpath("/stockQuote[price=NA]").doesNotExist());
	}	
		
	@Test
	public void listQuoteByReqParam() throws Exception {
		// Given
		final String inputCodeList = "2828,2800";		
		StockQuote quote = new StockQuote("HSCEI");
        quote.setPrice("10368.13");
        Mockito.when(mockWebQueryService.parse(Mockito.any(HSINetParser.class))).thenReturn(Optional.of(quote));
        
		MvcResult mvcResult = mockMvc.perform(get("/quote/list?codeList=" + inputCodeList).characterEncoding("utf-8"))
		.andExpect(status().isOk())
		.andExpect(view().name("quote/list")).andReturn();
		
		ModelMap modelMap = mvcResult.getModelAndView().getModelMap();
		List<StockQuote> indexes = (List<StockQuote>) modelMap.get("indexes");
		List<StockQuote> quotes = (List<StockQuote>) modelMap.get("quotes");
				
		assertTrue(modelMap.get("codeList").equals(inputCodeList));
		assertNotNull(modelMap.get("tbase"));
		assertNotNull(modelMap.get("tminus1"));
		assertNotNull(modelMap.get("tminus7"));
		assertNotNull(modelMap.get("tminus30"));
		assertNotNull(modelMap.get("tminus60"));
		
		assertTrue(!"NA".equals(quotes.get(0).getPrice()));
		assertTrue(StringUtils.isNotBlank(quotes.get(0).getStockCode()));
		assertTrue(!"NA".equals(quotes.get(1).getPrice()));
		assertTrue(StringUtils.isNotBlank(quotes.get(1).getStockCode()));
		
		assertTrue(!"NA".equals(quotes.get(0).getPrice()));
		assertTrue(StringUtils.isNotBlank(quotes.get(0).getStockCode()));
		assertTrue(!"NA".equals(quotes.get(1).getPrice()));
		assertTrue(StringUtils.isNotBlank(quotes.get(1).getStockCode()));
	}
	
	@Test
	public void createHoldingStockWithWrongMessage() throws Exception {
		// Given
		StockQuote quote = new StockQuote("HSCEI");
        quote.setPrice("10368.13");
        Mockito.when(mockWebQueryService.parse(Mockito.any(HSINetParser.class))).thenReturn(Optional.of(quote));
        
		String scbSellMsg = "渣打: (沽出10,000股01138.中海發展股份) \n";
		scbSellMsg += "已於4.8900元執行\n";
		scbSellMsg += "20140610000013235"; 
		
		MvcResult mvcResult = mockMvc.perform(
					post("/quote/createholdingstock").characterEncoding("utf-8")
					.param("message", scbSellMsg)
				).andExpect(status().isOk())
				.andExpect(model().attribute("resultMessage", "Cannot create holding stock"))
				.andExpect(view().name("quote/createholdingstock")).andReturn();
	}
	
	@Test
	public void createHoldingStockSuccessfully() throws Exception {
		// Given
		StockQuote quote = new StockQuote("HSCEI");
        quote.setPrice("10368.13");
        Mockito.when(mockWebQueryService.parse(Mockito.any(HSINetParser.class))).thenReturn(Optional.of(quote));        
		String scbSellMsg = "渣打: (沽出10,000股01138.中海發展股份) \n";
		scbSellMsg += "已於4.8900元成功執行\n";
		scbSellMsg += "20140710000013235"; 
		
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
		assertEquals(new BigDecimal("48900"), holdingStock.getGross());
		assertEquals(SquoteConstants.Side.SELL, holdingStock.getSide());
		assertEquals("10368.13", holdingStock.getHsce().toString());		
	}
	
	@Test
	public void failCreateHoldingStockDueToCannotParseHscei() throws Exception {		
		// Given
		Mockito.when(mockWebQueryService.parse(Mockito.any(HSINetParser.class))).thenReturn(Optional.<StockQuote>absent());        
		String scbSellMsg = "渣打: (沽出10,000股01138.中海發展股份) \n";
		scbSellMsg += "已於4.8900元成功執行\n";
		scbSellMsg += "20180610000013235"; 
		
		MvcResult mvcResult = mockMvc.perform(
					post("/quote/createholdingstock").characterEncoding("utf-8")
					.param("message", scbSellMsg)
				).andExpect(status().isOk())
				.andExpect(model().attribute("resultMessage", "Cannot get hscei"))
				.andExpect(view().name("quote/createholdingstock")).andReturn();		
	}
	
	
}