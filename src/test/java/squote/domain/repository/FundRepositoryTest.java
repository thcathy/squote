package squote.domain.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import squote.IntegrationTest;
import squote.domain.Fund;
import squote.domain.FundHolding;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class FundRepositoryTest extends IntegrationTest {
	private final Logger log = LoggerFactory.getLogger(FundRepositoryTest.class);
	private final String FUND_NAME = "Winning Fund";
	private final String USER_ID = "tester";

	@Autowired FundRepository repo;
		
	private Fund createSimpleFund() {
		Fund f1 = new Fund("tester", FUND_NAME);
		f1.buyStock("2828", BigDecimal.valueOf(400), new BigDecimal("40000"));
		f1.buyStock("2828", BigDecimal.valueOf(1000), new BigDecimal("100000"));
		return f1;
	}
	
	@AfterEach
	public void clearFund() {
		repo.deleteAll();
	}
	
	@Test
	public void sellStock_GivenAFund_ShouldDecreaseQtyAndGrossButPriceUnchange() {
		Fund f = createSimpleFund();
		BigDecimal sellQty = BigDecimal.valueOf(200);
		int sellGross = 25000;
		BigDecimal orgPrice = f.getHoldings().get("2828").getPrice();
		int expGross = f.getHoldings().get("2828").getGross().intValue() - orgPrice.multiply(sellQty).intValue();
		BigDecimal expQty = f.getHoldings().get("2828").getQuantity().subtract(sellQty);
		
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
			Fund f2 = repo.findByUserIdAndName(USER_ID, FUND_NAME).get();
			f2.getHoldings().get("2828").spotPrice();
		} catch (IllegalStateException e) {
			return;
		}
		fail("Spot Price should not ready when fund retrieve from db");
		
	}
	
	@Test
	@Rollback(false)
	public void save_ShouldWorks() {
		Fund myFund = new Fund(USER_ID, "New Fund");
		myFund.buyStock("1138", BigDecimal.valueOf(20000), new BigDecimal(70800));
		myFund.buyStock("288", BigDecimal.valueOf(20000), new BigDecimal(23450));
		myFund.buyStock("883", BigDecimal.valueOf(13000), new BigDecimal(176420));
		myFund.buyStock("1138", BigDecimal.valueOf(10000), new BigDecimal(58800));
		repo.save(myFund);
		//repo.delete(myFund);
	}
	
	

}
