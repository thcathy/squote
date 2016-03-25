package squote.controller.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import squote.SpringQuoteWebApplication;
import squote.SquoteConstants;
import squote.SquoteConstants.IndexCode;
import squote.SquoteConstants.Side;
import squote.domain.Fund;
import squote.domain.HoldingStock;
import squote.domain.StockQuote;
import squote.domain.repository.FundRepository;
import squote.domain.repository.HoldingStockRepository;
import squote.service.CentralWebQueryService;
import squote.web.parser.EtnetIndexQuoteParser;
import squote.web.parser.HSINetParser;
import thc.util.DateUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = SpringQuoteWebApplication.class)
@ActiveProfiles("dev")
public class CreateHoldingControllerIntegrationTest {
	@Mock CentralWebQueryService mockWebQueryService;
	@Autowired CreateHoldingController controller;
	@Autowired FundRepository fundRepo;
	@Autowired HoldingStockRepository holdingRepo;
	
	@Rule
	public ExpectedException expectedException = ExpectedException.none();
	private MockMvc mockMvc;
	private Fund testFund = createSimpleFund();
	private StockQuote hsceiQuote;
	private StockQuote hsiQuote;
	private static String HSI_PRICE = "25000";
	private static String HSCEI_PRICE = "12500";

	private Fund createSimpleFund() {
		Fund f = new Fund("testfund");
		f.buyStock("2828", 500, new BigDecimal(50000));
		f.buyStock("2800", 1000, new BigDecimal(25000));
		return f;
	}
	
	private HoldingStock createSell2800Holding() {
		return new HoldingStock("2800", Side.SELL, 300, new BigDecimal("8190"), new Date(), null);
	}

	@Before
	public void setup() {
		fundRepo.save(testFund);
		MockitoAnnotations.initMocks(this);
    	controller.webQueryService = mockWebQueryService;
		this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
		
		hsiQuote = new StockQuote(IndexCode.HSI.name);
        hsiQuote.setPrice(HSI_PRICE);
        
        hsceiQuote = new StockQuote(IndexCode.HSCEI.name);
        hsceiQuote.setPrice(HSCEI_PRICE);
	}

	@After
	public void revert() {
		fundRepo.delete(testFund);
	}

	@Test
	public void createHoldingStockFromExecution_givenCorrectMsg_shouldReturnSuccessful() {
		long holdingQty = holdingRepo.count();
		String hscei = "10000";
		String scbSellMsg = "渣打:買入6000股883.HK 中國海洋石油\n";
		scbSellMsg += "已完成\n";
		scbSellMsg += "平均價HKD7.99\n";
		scbSellMsg += "O1512110016740"; 
		
		HoldingStock holding = controller.createHoldingFromExecution(scbSellMsg, hscei);
		assertEquals(6000, holding.getQuantity());
		assertEquals(Side.BUY, holding.getSide());
		assertEquals("883", holding.getCode());
		assertEquals(new BigDecimal(hscei), holding.getHsce());
		assertEquals(holdingQty + 1, holdingRepo.count());
	}
	
	@Test
	public void createHolding_givenEmptyMsg_shouldThrowException() {
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("Cannot create holding");
		
		controller.createHoldingFromExecution("", null);
	}
	
	@Test
	public void createHolding_givenWrongMsg_shouldThrowException() {
		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("Cannot create holding");
		
		controller.createHoldingFromExecution("渣打 but it is wrong messags", null);
	}
	
	@Test
	public void createHolding_whenCannotGetHcei_shouldThrowException() {
		Mockito.when(mockWebQueryService.parse(Mockito.any(HSINetParser.class))).thenReturn(Optional.empty()); 
		String scbSellMsg = "渣打:買入6000股883.HK 中國海洋石油\n";
		scbSellMsg += "已完成\n";
		scbSellMsg += "平均價HKD7.99\n";
		scbSellMsg += "O1512110016740"; 
		expectedException.expect(RuntimeException.class);
		expectedException.expectMessage("Cannot get hcei");
		
		controller.createHoldingFromExecution(scbSellMsg, "0");
	}
	
	@Test	
	public void postCreateHoldingStock_GivenTodayExeMsg_ShouldCreateHoldingStockUseIndexFromRealtimeQuote() throws Exception {
		// Given
        Mockito.when(mockWebQueryService.parse(Mockito.any(EtnetIndexQuoteParser.class))).thenReturn(Optional.of(Arrays.asList(hsiQuote, hsceiQuote)));
        String scbSellMsg = "渣打:買入6000股883.HK 中國海洋石油\n";
		scbSellMsg += "已完成\n";
		scbSellMsg += "平均價HKD7.99\n";
		scbSellMsg += "O" + DateUtils.toString(new Date(), "yyMMdd") + "00013235";
		long totalHoldings = holdingRepo.count();
		
		// Expect
		HoldingStock holding = controller.createHoldingFromExecution(scbSellMsg, "0");
		assertNotNull(holding);
		assertEquals("883", holding.getCode());
		assertEquals(6000, holding.getQuantity());
		assertEquals(new BigDecimal("47940.00"), holding.getGross());
		assertEquals(SquoteConstants.Side.BUY, holding.getSide());
		assertEquals(HSCEI_PRICE, holding.getHsce().toString());
		assertEquals(totalHoldings+1, holdingRepo.count());
	}
	
	@Test
	public void updatefund_GivenHoldingStock_ShouldUpdateAndPersist() throws Exception {
		HoldingStock holding = holdingRepo.save(createSell2800Holding());

		controller.updateFundByHolding(testFund.name, holding.getId());
		Fund fund = fundRepo.findOne(testFund.name);
		assertNotNull(fund);
		assertEquals(700, fund.getHoldings().get(holding.getCode()).getQuantity());
	}
}
