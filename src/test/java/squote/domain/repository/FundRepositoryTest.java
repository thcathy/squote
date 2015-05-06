package squote.domain.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigDecimal;

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

import squote.SpringQuoteWebApplication;
import squote.domain.Fund;
import squote.domain.FundHolding;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringQuoteWebApplication.class)
@ActiveProfiles("dev")
public class FundRepositoryTest {
	private Logger log = LoggerFactory.getLogger(FundRepositoryTest.class);
	private final String FUND_NAME = "Winning Fund";
	
	@Autowired FundRepository repo;
	
	@Before
	public void Fund() {
		Fund f1 = createFund1();
		repo.save(f1);	
	}

	private Fund createFund1() {
		Fund f1 = new Fund(FUND_NAME);
		f1.buyStock("2828", 400, new BigDecimal("40000"));
		f1.buyStock("2828", 1000, new BigDecimal("95000"));
		return f1;
	}
	
	@After
	public void clearFund() {
		repo.deleteAll();
	}
	
	@Test
	public void selectAndUpdate_GivenFundObj_ShouldFindAndUpdateSuccessfully() throws Exception {				
		Fund f = repo.findOne(FUND_NAME);
		log.debug("Found Winning Fund: {}", f);
		assertEquals(1400, f.getHoldings().get("2828").getQuantity());
		assertEquals(new BigDecimal("135000"), f.getHoldings().get("2828").getGross());
		assertNotNull(f.getHoldings().get("2828").getDate());
		
		f.sellStock("2828", 200, new BigDecimal("20000"));
		repo.save(f);
		
		Fund f2 = repo.findOne(FUND_NAME);
		log.debug("Found updated Winning Fund: {}", f2);
		assertEquals(1200, f2.getHoldings().get("2828").getQuantity());
		assertNotNull(f2.getHoldings().get("2828").getDate());
	}	
	
	@Test
	public void sellStock_GivenAFund_ShouldDecreaseQtyAndGrossButPriceUnchange() {
		Fund f = createFund1();
		int sellQty = 200;
		int sellGross = 25000;
		BigDecimal orgPrice = f.getHoldings().get("2828").getPrice();
		int expGross = f.getHoldings().get("2828").getGross().intValue() - orgPrice.multiply(new BigDecimal(sellQty)).intValue();
		int expQty = f.getHoldings().get("2828").getQuantity() - sellQty;
		
		f.sellStock("2828", sellQty, new BigDecimal(25000));
		
		FundHolding hcei = f.getHoldings().get("2828");
		assertEquals(expQty, hcei.getQuantity());
		assertEquals(expGross, hcei.getGross().intValue());
		assertEquals(orgPrice, hcei.getPrice());
		
	}
}
