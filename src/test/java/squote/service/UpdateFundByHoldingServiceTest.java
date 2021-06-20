package squote.service;

import com.binance.api.client.domain.account.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import squote.SquoteConstants.Side;
import squote.domain.Fund;
import squote.domain.FundHolding;
import squote.domain.HoldingStock;
import squote.domain.repository.FundRepository;
import squote.domain.repository.HoldingStockRepository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
 
public class UpdateFundByHoldingServiceTest {
	UpdateFundByHoldingService service;
	HoldingStock buy883 = createBuyHolding();
	HoldingStock sell2800 = createSell2800Holding();
	HoldingStock sellAll2828 = createSellAll2828Holding();
	Fund fund = createStockFund();
	Fund cryptoFund = createCryptoFund();
	BinanceAPIService mockBinanceAPIService;
	
	@BeforeEach
	public void setupService() {		
		FundRepository mockFundRepo = Mockito.mock(FundRepository.class);
		when(mockFundRepo.findByUserIdAndName(fund.userId, fund.name)).thenReturn(Optional.of(fund));
		when(mockFundRepo.findByUserIdAndName(cryptoFund.userId, cryptoFund.name)).thenReturn(Optional.of(cryptoFund));
				
		HoldingStockRepository mockHoldingRepo = Mockito.mock(HoldingStockRepository.class);
		when(mockHoldingRepo.findById(buy883.getId())).thenReturn(Optional.of(buy883));
		when(mockHoldingRepo.findById(sell2800.getId())).thenReturn(Optional.of(sell2800));
		when(mockHoldingRepo.findById(sellAll2828.getId())).thenReturn(Optional.of(sellAll2828));

		mockBinanceAPIService = Mockito.mock(BinanceAPIService.class);
		
		service = new UpdateFundByHoldingService(mockFundRepo, mockHoldingRepo, mockBinanceAPIService);
	}
	
	@Test
	public void update_givenBuyHolding_shouldAddStockToFund() {
		Fund f = service.updateFundByHolding(fund.userId, fund.name, buy883.getId());
		
		FundHolding fundHolding = f.getHoldings().get(buy883.getCode());
		assertEquals(new BigDecimal("78800"), fundHolding.getGross());
		assertEquals(6500, fundHolding.getQuantity());
	}
	
	@Test
	public void update_givenSell2800_shouldReduceStockAndUpdateProfitToFund() {
		Fund f = service.updateFundByHolding(fund.userId, fund.name, sell2800.getId());
		
		FundHolding fundHolding = f.getHoldings().get(sell2800.getCode());
		assertEquals(690, f.getProfit().doubleValue(), 0);
		assertEquals(17500, fundHolding.getGross().doubleValue(), 0);
		assertEquals(700, fundHolding.getQuantity());
	}	
	
	@Test
	public void update_givenSellAll2828_shouldRemoveStockAndUpdateProfitToFund() {
		Fund f = service.updateFundByHolding(fund.userId, fund.name, sellAll2828.getId());
				
		assertEquals(-6789.5, f.getProfit().doubleValue(), 0);
		assertNull(f.getHoldings().get(sellAll2828.getCode()));
	}

	@Test
	public void getTrades_shouldUpdateToFund() {
		when(mockBinanceAPIService.getMyTrades("BTCUSDT")).thenReturn(createBTCTrades());

		var f = service.getTradesAndUpdateFund(cryptoFund.userId, cryptoFund.name, "binance");
		var BTCHolding = f.getHoldings().get("BTCUSDT");
		assertEquals(BigDecimal.valueOf(0.025), BTCHolding.getQuantity());
		assertEquals(BigDecimal.valueOf(186.05), BTCHolding.getGross());
		assertEquals(BigDecimal.ZERO, f.getHoldings().get("ETHUSDT").getQuantity());
	}

	private List<Trade> createBTCTrades() {
		Trade trade1 = new Trade();
		trade1.setSymbol("BTCUSDT");
		trade1.setQty("0.01");
		trade1.setQuoteQty("63.04");
		trade1.setTime(1617782100511L);
		Trade trade2 = new Trade();
		trade2.setSymbol("BTCUSDT");
		trade2.setQty("0.015");
		trade2.setQuoteQty("123.01");
		trade2.setTime(1617783725934L);
		Trade trade3 = new Trade();
		trade3.setSymbol("BTCUSDT");
		trade3.setQty("0.015");
		trade3.setQuoteQty("123.01");
		trade3.setTime(1617700000511L);
		return List.of(trade1, trade2, trade3);
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
	
	private Fund createStockFund() {
		Fund f = new Fund("tester", "testfund");
		f.buyStock("2828", BigDecimal.valueOf(500), BigDecimal.valueOf(50000));
		f.buyStock("2800", BigDecimal.valueOf(1000), BigDecimal.valueOf(25000));
		f.buyStock("883", BigDecimal.valueOf(500), BigDecimal.valueOf(5000));
		return f;
	}

	private Fund createCryptoFund() {
		Fund f = new Fund("tester", "cryptofund");
		f.setType(Fund.FundType.CRYPTO);
		f.buyStock("BTCUSDT", BigDecimal.ZERO, BigDecimal.ZERO);
		f.buyStock("ETHUSDT", BigDecimal.ZERO, BigDecimal.ZERO);
		f.setLatestTradeTime(1617782100000L);
		return f;
	}
}
