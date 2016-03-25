package squote.service;
 
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import squote.SquoteConstants.Side;
import squote.domain.Fund;
import squote.domain.FundHolding;
import squote.domain.HoldingStock;
import squote.domain.repository.FundRepository;
import squote.domain.repository.HoldingStockRepository;
 
public class UpdateFundByHoldingTest {
	UpdateFundByHoldingService service;
	HoldingStock buy883 = createBuyHolding();
	HoldingStock sell2800 = createSell2800Holding();
	HoldingStock sellAll2828 = createSellAll2828Holding();
	Fund fund = createSimpleFund();
	
	@Before
	public void setupService() {		
		FundRepository mockFundRepo = Mockito.mock(FundRepository.class);
		when(mockFundRepo.findOne(fund.name)).thenReturn(fund);
				
		HoldingStockRepository mockHoldingRepo = Mockito.mock(HoldingStockRepository.class);
		when(mockHoldingRepo.findOne(buy883.getId())).thenReturn(buy883);
		when(mockHoldingRepo.findOne(sell2800.getId())).thenReturn(sell2800);
		when(mockHoldingRepo.findOne(sellAll2828.getId())).thenReturn(sellAll2828);
		
		service = new UpdateFundByHoldingService(mockFundRepo, mockHoldingRepo);
	}
	
	@Test
	public void update_givenBuyHolding_shouldAddStockToFund() {
		Fund f = service.updateFundByHolding(fund.name, buy883.getId());
		
		FundHolding fundHolding = f.getHoldings().get(buy883.getCode());
		assertEquals(new BigDecimal("78800"), fundHolding.getGross());
		assertEquals(6500, fundHolding.getQuantity());
	}
	
	@Test
	public void update_givenSell2800_shouldReduceStockAndUpdateProfitToFund() {
		Fund f = service.updateFundByHolding(fund.name, sell2800.getId());
		
		FundHolding fundHolding = f.getHoldings().get(sell2800.getCode());
		assertEquals(690, f.getProfit().doubleValue(), 0);
		assertEquals(17500, fundHolding.getGross().doubleValue(), 0);
		assertEquals(700, fundHolding.getQuantity());
	}	
	
	@Test
	public void update_givenSellAll2828_shouldRemoveStockAndUpdateProfitToFund() {
		Fund f = service.updateFundByHolding(fund.name, sellAll2828.getId());
				
		assertEquals(-6789.5, f.getProfit().doubleValue(), 0);
		assertNull(f.getHoldings().get(sellAll2828.getCode()));
	}
	
	private HoldingStock createBuyHolding() {		
		return new HoldingStock("1", "883", Side.BUY, 6000, new BigDecimal("73800"), new Date(), null);
	}
	
	private HoldingStock createSell2800Holding() {
		return new HoldingStock("2", "2800", Side.SELL, 300, new BigDecimal("8190"), new Date(), null);
	}
	
	private HoldingStock createSellAll2828Holding() {
		return new HoldingStock("3", "2828", Side.SELL, 500, new BigDecimal("43210.5"), new Date(), null);
	}
	
	private Fund createSimpleFund() {
		Fund f = new Fund("testfund");
		f.buyStock("2828", 500, new BigDecimal(50000));
		f.buyStock("2800", 1000, new BigDecimal(25000));
		f.buyStock("883", 500, new BigDecimal(5000));
		return f;
	}
}