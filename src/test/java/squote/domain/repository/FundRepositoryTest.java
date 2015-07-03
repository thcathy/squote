package squote.domain.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigDecimal;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
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
		
	private Fund createSimpleFund() {
		Fund f1 = new Fund(FUND_NAME);
		f1.buyStock("2828", 400, new BigDecimal("40000"));
		f1.buyStock("2828", 1000, new BigDecimal("100000"));
		return f1;
	}
	
	@After
	public void clearFund() {
		repo.deleteAll();
	}
	
	@Test
	public void sellStock_GivenAFund_ShouldDecreaseQtyAndGrossButPriceUnchange() {
		Fund f = createSimpleFund();
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

	@Test
	public void save_ShouldNotPersistSpotPriceInFundHolding() {
		Fund f1 = createSimpleFund();
		BigDecimal price2828 = new BigDecimal(125);
		f1.getHoldings().get("2828").calculateNetProfit(price2828);
		repo.save(f1);		
		assertEquals(price2828, f1.getHoldings().get("2828").spotPrice());		
		
		try {
			Fund f2 = repo.findOne("Winning Fund");
			f2.getHoldings().get("2828").spotPrice();
		} catch (IllegalStateException e) {
			return;
		}
		fail("Spot Price should not ready when fund retrieve from db");
		
	}
	
	@Test
	@Rollback(false)
	public void save_ShouldWorks() {
		Fund myFund = new Fund("New Fund");
		myFund.buyStock("1138", 20000, new BigDecimal(70800));
		myFund.buyStock("288", 20000, new BigDecimal(23450));
		myFund.buyStock("883", 13000, new BigDecimal(176420));
		myFund.buyStock("1138", 10000, new BigDecimal(58800));
		repo.save(myFund);
		//repo.delete(myFund);
	}
	
	

}
