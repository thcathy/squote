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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

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
import squote.SquoteConstants.IndexCode;
import squote.domain.HoldingStock;
import squote.domain.StockQuote;
import squote.service.CentralWebQueryService;
import squote.web.parser.EtnetIndexQuoteParser;
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
	private StockQuote hsceiQuote;
	private StockQuote hsiQuote;
	    
    @Before
    public void setup() {    	
    	MockitoAnnotations.initMocks(this);
    	quoteController.webQueryService = mockWebQueryService;
        this.mockMvc = MockMvcBuilders.standaloneSetup(quoteController).build();
        
        hsiQuote = new StockQuote(IndexCode.HSI.name);
        hsiQuote.setPrice("25000");
        
        hsceiQuote = new StockQuote(IndexCode.HSCEI.name);
        hsceiQuote.setPrice("12500");
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
		Future<Optional<List<StockQuote>>> mockIndexQuoteFuture = Mockito.mock(Future.class);
        Mockito.when(mockWebQueryService.parse(Mockito.any(HSINetParser.class))).thenReturn(Optional.of(hsceiQuote));
        Mockito.when(mockIndexQuoteFuture.get()).thenReturn(Optional.of(Arrays.asList(hsiQuote, hsceiQuote)));
        Mockito.when(mockWebQueryService.submit(Mockito.any(EtnetIndexQuoteParser.class))).thenReturn(mockIndexQuoteFuture);
        
		MvcResult mvcResult = mockMvc.perform(get("/quote/list?codeList=" + inputCodeList).characterEncoding("utf-8"))
		.andExpect(status().isOk())
		.andExpect(view().name("quote/list")).andReturn();
		
		ModelMap modelMap = mvcResult.getModelAndView().getModelMap();
		List<StockQuote> indexes = (List<StockQuote>) modelMap.get("indexes");
		List<StockQuote> quotes = (List<StockQuote>) modelMap.get("quotes");
		StockQuote hscei = (StockQuote) modelMap.get("hsce");
				
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
		
		assertEquals(IndexCode.HSCEI.name, hscei.getStockCode());
		assertEquals("12500", hscei.getPrice());
	}
}