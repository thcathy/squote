package squote.domain;

import java.math.BigDecimal;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import squote.SpringQuoteWebApplication;
import squote.SquoteConstants.Side;
import squote.domain.repository.FundRepository;
import squote.domain.repository.HoldingStockRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringQuoteWebApplication.class)
@ActiveProfiles("dev")
public class SquoteDataTest {
	@Autowired HoldingStockRepository holdingStockRepo;
	@Autowired FundRepository fundRepo;
	
	@Before
	public void clearCollections() {
		holdingStockRepo.deleteAll();
		fundRepo.deleteAll();
	}
	
	@Test
    public void createEntityForIntegrationTest() {
		
		holdingStockRepo.save(createHoldingStock1());
		holdingStockRepo.save(createHoldingStock2());
		
		fundRepo.save(createSimpleFund1());
		fundRepo.save(createSimpleFund2());
		
    }
	
	private HoldingStock createHoldingStock1() {
		String scbSellMsg = "渣打:買入6000股883.HK 中國海洋石油\n";
		scbSellMsg += "已完成\n";
		scbSellMsg += "平均價HKD7.99\n";
		scbSellMsg += "O1512110016740"; 
	
		StockExecutionMessage msg = StockExecutionMessage.construct(scbSellMsg).get();
		return HoldingStock.from(msg, new BigDecimal("123"));
	}
	
	private HoldingStock createHoldingStock2() {
		return new HoldingStock("941", Side.SELL, 2000, new BigDecimal(103872), new Date(), new BigDecimal(11385.23));
	}
	
	private Fund createSimpleFund1() {
		Fund f = new Fund("testfund");
		f.buyStock("2828", 500, new BigDecimal("50000"));
		f.buyStock("2800", 1000, new BigDecimal("25000"));
		f.setProfit(new BigDecimal("888"));
		return f;
	}
	
	private Fund createSimpleFund2() {
		Fund f = new Fund("testfund2");
		f.buyStock("1138", 54000, new BigDecimal("242466.20"));
		f.buyStock("2800", 1000, new BigDecimal("25000"));
		return f;
	}
}
