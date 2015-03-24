package squote.domain.repository;

import static org.junit.Assert.*;

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

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringQuoteWebApplication.class)
@ActiveProfiles("dev")
public class FundRepositoryTest {
	private Logger log = LoggerFactory.getLogger(FundRepositoryTest.class);
	@Autowired FundRepository repo;
	
	@Before
	public void createFund() {
		Fund f1 = new Fund("Winning Fund");
		f1.buyStock("2828", 400, new BigDecimal("40000"));
		f1.buyStock("2828", 1000, new BigDecimal("95000"));
		repo.save(f1);	
	}
	
	@After
	public void clearFund() {
		repo.deleteAll();
	}
	
	@Test
	public void selectAndUpdate_GivenFundObj_ShouldFindAndUpdateSuccessfully() throws Exception {				
		Fund f = repo.findOne("Winning Fund");
		log.debug("Found Winning Fund: {}", f);
		assertEquals(1400, f.getHoldings().get("2828").getQuantity());
		assertEquals(new BigDecimal("135000"), f.getHoldings().get("2828").getGross());
		assertNotNull(f.getHoldings().get("2828").getDate());
		
		f.sellStock("2828", 200, new BigDecimal("20000"));
		repo.save(f);
		
		Fund f2 = repo.findOne("Winning Fund");
		log.debug("Found updated Winning Fund: {}", f2);
		assertEquals(1200, f2.getHoldings().get("2828").getQuantity());
		assertEquals(new BigDecimal("115000"), f2.getHoldings().get("2828").getGross());
		assertNotNull(f2.getHoldings().get("2828").getDate());
	}	
}
