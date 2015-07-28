package squote.controller;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

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
import squote.controller.rest.FundController;
import squote.domain.Fund;
import squote.domain.repository.FundRepository;

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
		mockMvc.perform(get("/rest/fund/testfund/buy/2800/100/100"))
				.andExpect(status().isOk());
				
		Fund result = fundRepo.findOne("testfund");
		assertEquals(1100, result.getHoldings().get("2800").getQuantity());
		assertEquals(35000, result.getHoldings().get("2800").getGross().intValue());
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
}
