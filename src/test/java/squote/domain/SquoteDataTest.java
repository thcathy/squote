package squote.domain;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import squote.SpringQuoteWebApplication;
import squote.domain.repository.HoldingStockRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringQuoteWebApplication.class)
@ActiveProfiles("dev")
public class SquoteDataTest {
	@Autowired HoldingStockRepository holdingStockRepo;
	
	@Before
	public void clearCollections() {
		holdingStockRepo.deleteAll();
	}
	
	@Test
    public void createEntityForIntegrationTest() {
		String scbBuyMsg = "渣打: (買入10,000股01138.中海發展股份) \n";
		scbBuyMsg += "已於5.2300元成功執行\n";
		scbBuyMsg += "20150205000020865";
		
		StockExecutionMessage msg = StockExecutionMessage.construct(scbBuyMsg).get();
		HoldingStock holdingStock = HoldingStock.from(msg, new BigDecimal("123"));
		holdingStockRepo.save(holdingStock);
		
    }
}
