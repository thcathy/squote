package squote.controller.rest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import squote.SpringQuoteWebApplication;
import squote.domain.Fund;
import squote.domain.FundHolding;
import squote.domain.repository.FundRepository;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = SpringQuoteWebApplication.class)
@ActiveProfiles("dev")
public class FundControllerIntegrationTest {
	@Autowired FundController fundController;
	@Autowired FundRepository fundRepo;

	private MockMvc mockMvc;
	private Fund testFund = createSimpleFund();

	private Fund createSimpleFund() {
		Fund f = new Fund("testfund");
		f.buyStock("2828", 500, new BigDecimal(50000));
		f.buyStock("2800", 1000, new BigDecimal(25000));
		return f;
	}

	@Before
	public void setup() {
		this.mockMvc = MockMvcBuilders.standaloneSetup(fundController).build();
		fundRepo.save(testFund);
	}

	@After
	public void revert() {
		fundRepo.delete(testFund);
	}

	@Test
	public void urlbuy_addStockAndSave() throws Exception {
		mockMvc.perform(get("/rest/fund/testfund/buy/2800/100/100.1/"))
				.andExpect(status().isOk());
				
		Fund result = fundRepo.findOne("testfund");
		assertEquals(1100, result.getHoldings().get("2800").getQuantity());
		assertEquals(35010, result.getHoldings().get("2800").getGross().intValue());
	}

	@Test
	public void urlsell_minusStockAndSave() throws Exception {
		mockMvc.perform(get("/rest/fund/testfund/sell/2828/100/120")).andExpect(
				status().isOk());
		
		Fund result = fundRepo.findOne("testfund");
		assertEquals(400, result.getHoldings().get("2828").getQuantity());
		assertEquals(40000, result.getHoldings().get("2828").getGross().intValue());	// gross is deduce base on original gross price
	}
	
	@Test
	public void urlcreate_givenNewName_shouldCreateNewFund() throws Exception {
		mockMvc.perform(get("/rest/fund/create/newfund"))
				.andExpect(status().isOk());
		
		Fund result = fundRepo.findOne("newfund");
		assertEquals("newfund", result.name);
		assertEquals(0, result.getProfit().intValue());
	}
	
	@Test
	public void urlDelete_ShouldRemoveFund() throws Exception {
		mockMvc.perform(get("/rest/fund/delete/testfund"))
			.andExpect(status().isOk());
			
		assertEquals(null, fundRepo.findOne("testfund"));
	}
	
	@Test
	public void urlremove_ShouldRemoveAStockFromFund() throws Exception {
		mockMvc.perform(get("/rest/fund/testfund/remove/2800"))
				.andExpect(status().isOk());
		
		Fund result = fundRepo.findOne("testfund");
		assertEquals(null, result.getHoldings().get("2800"));
	}
	
	@Test
	public void payInterest_ShouldSaveSubstractedFund() throws Exception {
		mockMvc.perform(get("/rest/fund/testfund/payinterest/2828/100.5/"))
			.andExpect(status().isOk());
		
		Fund fund = fundRepo.findOne("testfund");
		FundHolding newHolding = fund.getHoldings().get("2828");
		
		assertEquals(500, newHolding.getQuantity());		
		assertEquals(new BigDecimal("49899.5"), newHolding.getGross());			
		assertEquals(new BigDecimal("99.7990"), newHolding.getPrice());
	}
	
	@Test
	public void addProfit_givenListOfValue_shouldAddedToProfit() throws Exception {
		mockMvc.perform(get("/rest/fund/testfund/add/profit/101,50.5/"))
			.andExpect(status().isOk());
		
		mockMvc.perform(get("/rest/fund/testfund/add/profit/98.2/"))
		.andExpect(status().isOk());
		
		Fund fund = fundRepo.findOne("testfund");
		assertEquals(new BigDecimal("249.7"), fund.getProfit());
	}
	
	@Test
	public void addExpense_givenListOfValue_shouldMinusFromProfit() throws Exception {
		mockMvc.perform(get("/rest/fund/testfund/subtract/profit/30,2.32,.89/"))
			.andExpect(status().isOk());
		
		mockMvc.perform(get("/rest/fund/testfund/subtract/profit/5/"))
		.andExpect(status().isOk());
		
		Fund fund = fundRepo.findOne("testfund");
		assertEquals(new BigDecimal("-38.21"), fund.getProfit());
	}
	
	@Test
	public void setProfit_givenValue_shouldBeTheProfit() throws Exception {
		mockMvc.perform(get("/rest/fund/testfund/set/profit/123.456/"))
			.andExpect(status().isOk());
				
		Fund fund = fundRepo.findOne("testfund");
		assertEquals(new BigDecimal("123.456"), fund.getProfit());
	}
	
	@Test
	public void getAll_shouldReturnAllFunds() throws Exception {
		String result = mockMvc.perform(get("/rest/fund/getall/"))
			.andExpect(status().isOk())
			.andReturn().getResponse().getContentAsString();
		
		assertTrue(result.contains("newfund"));
		assertTrue(result.contains("testfund"));
	}

	@Test
	public void cashout_shouldAddToAmount() throws Exception {
		mockMvc.perform(get("/rest/fund/testfund/cashout/123.456/"))
				.andExpect(status().isOk());

		Fund fund = fundRepo.findOne("testfund");
		assertEquals(new BigDecimal("123.456"), fund.getCashoutAmount());
	}
}
