package squote.controller;
 
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

import java.util.Iterator;
import java.util.List;

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
import squote.SquoteConstants.IndexCode;
import squote.domain.StockQuote;
 
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = SpringQuoteWebApplication.class)
@ActiveProfiles("dev")
public class QuoteControllerIntegrationTest {
	@Autowired QuoteController quoteController;
	
	private MockMvc mockMvc;
	    
    @Before
    public void setup() {    	
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
		final String inputCodes = "753,2828,2800";
		        
		MvcResult mvcResult = mockMvc.perform(get("/quote/list?codes=" + inputCodes).characterEncoding("utf-8"))
		.andExpect(status().isOk())
		.andExpect(view().name("quote/list")).andReturn();
		
		ModelMap modelMap = mvcResult.getModelAndView().getModelMap();
		@SuppressWarnings("unchecked") List<StockQuote> indexes = (List<StockQuote>) modelMap.get("indexes");
		@SuppressWarnings("unchecked") Iterator<StockQuote> quotes = (Iterator<StockQuote>) modelMap.get("quotes");
		StockQuote hscei = (StockQuote) modelMap.get("hsce");
				
		assertTrue(modelMap.get("codes").equals(inputCodes));
		assertNotNull(modelMap.get("tbase"));
		assertNotNull(modelMap.get("tminus1"));
		assertNotNull(modelMap.get("tminus7"));
		assertNotNull(modelMap.get("tminus30"));
		assertNotNull(modelMap.get("tminus60"));
		
		StockQuote quote1 = quotes.next();
		StockQuote quote2 = quotes.next();
		assertTrue(!"NA".equals(quote1.getPrice()));
		assertEquals("753",quote1.getStockCode());
		assertTrue(!"NA".equals(quote2.getPrice()));
		assertEquals("2828",quote2.getStockCode());
				
		assertEquals(IndexCode.HSCEI.name, hscei.getStockCode());
		
		assertEquals(2, indexes.size());
	}
}
