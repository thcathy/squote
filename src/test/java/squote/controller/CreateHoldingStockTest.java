package squote.controller;
 
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import squote.SpringQuoteWebApplication;
import squote.SquoteConstants;
import squote.SquoteConstants.IndexCode;
import squote.domain.HoldingStock;
import squote.domain.StockQuote;
import squote.domain.repository.HoldingStockRepository;
import squote.service.CentralWebQueryService;
import squote.web.parser.EtnetIndexQuoteParser;
import squote.web.parser.HSINetParser;
import thc.util.DateUtils;
 
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = SpringQuoteWebApplication.class)
@ActiveProfiles("dev")
@DirtiesContext
public class CreateHoldingStockTest {
	@Mock CentralWebQueryService mockWebQueryService;
		
	@Autowired QuoteController quoteController;
	@Autowired HoldingStockRepository holdingStockRepo;
	
	private MockMvc mockMvc;
	private StockQuote hsceiQuote;
	private StockQuote hsiQuote;
	
	private static String HSI_PRICE = "25000";
	private static String HSCEI_PRICE = "12500";
    
    @Before
    public void setup() {    	
    	MockitoAnnotations.initMocks(this);
    	quoteController.webQueryService = mockWebQueryService;
        this.mockMvc = MockMvcBuilders.standaloneSetup(quoteController).build();
        
        hsiQuote = new StockQuote(IndexCode.HSI.name);
        hsiQuote.setPrice(HSI_PRICE);
        
        hsceiQuote = new StockQuote(IndexCode.HSCEI.name);
        hsceiQuote.setPrice(HSCEI_PRICE);
    }
    	
	@Test
	public void postCreateHoldingStock_GivenWrongExeMsg_ShouldShowMsgCannotCreate() throws Exception {
		// Given
		StockQuote quote = new StockQuote("HSCEI");
        quote.setPrice("10368.13");
        Mockito.when(mockWebQueryService.parse(Mockito.any(HSINetParser.class))).thenReturn(Optional.of(quote));
        
		String scbSellMsg = "渣打: (沽出10,000股01138.中海發展股份) \n";
		scbSellMsg += "已於4.8900元執行\n";
		scbSellMsg += "20140610000013235"; 
		
		mockMvc.perform(
					post("/quote/createholdingstock").characterEncoding("utf-8")
					.param("message", scbSellMsg)
				).andExpect(status().isOk())
				.andExpect(model().attribute("resultMessage", "Cannot create holding stock"))
				.andExpect(view().name("quote/createholdingstock")).andReturn();
	}
	
	@Test
	public void postCreateHoldingStock_GivenRightExeMsg_ShouldCreateHoldingStock() throws Exception {
		// Given		
		StockQuote quote = new StockQuote("HSCEI");
        quote.setPrice("10,368.13");
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
		assertEquals(new BigDecimal("48900.0000"), holdingStock.getGross());
		assertEquals(SquoteConstants.Side.SELL, holdingStock.getSide());
		assertEquals("10368.13", holdingStock.getHsce().toString());		
	}
	
	@Test
	public void postCreateHoldingStock_GivenCannotParseHSCEI_ShouldShowMsgCannotGetHSCEI() throws Exception {		
		// Given
		Mockito.when(mockWebQueryService.parse(Mockito.any(HSINetParser.class))).thenReturn(Optional.empty());        
		String scbSellMsg = "渣打: (沽出10,000股01138.中海發展股份) \n";
		scbSellMsg += "已於4.8900元成功執行\n";
		scbSellMsg += "20180610000013235"; 
		
		mockMvc.perform(
					post("/quote/createholdingstock").characterEncoding("utf-8")
					.param("message", scbSellMsg)
				).andExpect(status().isOk())
				.andExpect(model().attribute("resultMessage", "Cannot get hscei"))
				.andExpect(view().name("quote/createholdingstock")).andReturn();		
	}
	
	@Test
	public void postCreateHoldingStock_GivenEmptyInput_ShouldGoToCreateHoldingStockPage() throws Exception {
		mockMvc.perform(
				post("/quote/createholdingstock").characterEncoding("utf-8")
			).andExpect(status().isOk())
			.andExpect(model().attributeDoesNotExist("resultMessage"))
			.andExpect(view().name("quote/createholdingstock")).andReturn();
	}
	
	@Test	
	public void postCreateHoldingStock_GivenTodayExeMsg_ShouldCreateHoldingStockUseIndexFromRealtimeQuote() throws Exception {
		// Given
        Mockito.when(mockWebQueryService.parse(Mockito.any(EtnetIndexQuoteParser.class))).thenReturn(Optional.of(Arrays.asList(hsiQuote, hsceiQuote)));
        
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
		assertEquals(HSCEI_PRICE, holdingStock.getHsce().toString());
		assertEquals(total+1, holdingStockRepo.count());
	}
}