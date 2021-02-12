package squote.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import squote.IntegrationTest;
import squote.SquoteConstants.Side;
import squote.domain.repository.FundRepository;
import squote.domain.repository.HoldingStockRepository;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SquoteDataTest extends IntegrationTest {
	@Autowired HoldingStockRepository holdingStockRepo;
	@Autowired FundRepository fundRepo;

	private String userId = "SquoteDataTestUserId";

	@BeforeEach
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

		assertEquals(2, holdingStockRepo.findByUserIdOrderByDate(userId).size());
    }
	
	private HoldingStock createHoldingStock1() {
		String scbSellMsg = "渣打:買入6000股883.HK 中國海洋石油\n";
		scbSellMsg += "已完成\n";
		scbSellMsg += "平均價HKD7.99\n";
		scbSellMsg += "O1512110016740"; 
	
		StockExecutionMessage msg = StockExecutionMessage.construct(scbSellMsg).get();
		return HoldingStock.from(msg, userId, new BigDecimal("123"));
	}
	
	private HoldingStock createHoldingStock2() {
		return new HoldingStock(userId, "941", Side.SELL, 2000, new BigDecimal(103872), new Date(), new BigDecimal(11385.23));
	}
	
	private Fund createSimpleFund1() {
		Fund f = new Fund(userId, "testfund");
		f.buyStock("2828", 500, new BigDecimal("50000"));
		f.buyStock("2800", 1000, new BigDecimal("25000"));
		f.setProfit(new BigDecimal("888"));
		return f;
	}
	
	private Fund createSimpleFund2() {
		Fund f = new Fund(userId, "testfund2");
		f.buyStock("1138", 54000, new BigDecimal("242466.20"));
		f.buyStock("2800", 1000, new BigDecimal("25000"));
		return f;
	}
}
