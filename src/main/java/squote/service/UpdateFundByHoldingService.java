package squote.service;

import com.binance.api.client.domain.account.Trade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import squote.SquoteConstants.Side;
import squote.domain.Fund;
import squote.domain.FundHolding;
import squote.domain.HoldingStock;
import squote.domain.repository.FundRepository;
import squote.domain.repository.HoldingStockRepository;

import java.math.BigDecimal;
import java.util.List;

public class UpdateFundByHoldingService {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	final static String SOURCE_BINANCE = "binance";
		
	@Autowired FundRepository fundRepo;
	@Autowired HoldingStockRepository holdingRepo;
	@Autowired BinanceAPIService binanceAPIService;
		
	public UpdateFundByHoldingService(FundRepository fundRepo, HoldingStockRepository holdingRepo, BinanceAPIService binanceAPIService) {
		this.fundRepo = fundRepo;
		this.holdingRepo = holdingRepo;
		this.binanceAPIService = binanceAPIService;
	}
	
	public Fund updateFundByHoldingAndPersist(String userId, String fundName, String holdingId) {
		Fund f = updateFundByHolding(userId, fundName, holdingId);
		fundRepo.save(f);
		return f;
	}

	public Fund updateFundByHolding(String userId, String fundName, String holdingId) {
		Fund f = fundRepo.findByUserIdAndName(userId, fundName).get();
		HoldingStock holding = holdingRepo.findById(holdingId).get();
		
		if (Side.BUY.equals(holding.getSide()))
			f.buyStock(holding.getCode(), BigDecimal.valueOf(holding.getQuantity()), holding.getGross());
		else if (Side.SELL.equals(holding.getSide())) {
			updateProfit(f, holding);			
			f.sellStock(holding.getCode(), BigDecimal.valueOf(holding.getQuantity()), holding.getGross());
		}
		
		return f;
	}

	public Fund getTradesAndUpdateFund(String userId, String fundName, String source) {
		Fund f = fundRepo.findByUserIdAndName(userId, fundName).get();
		if (SOURCE_BINANCE.equalsIgnoreCase(source)) {
			long latestTradeTime = 0;
			for (String code : f.getHoldings().keySet()) {
				List<Trade> trades = binanceAPIService.getMyTrades(code);
				addTrades(f, trades);
				latestTradeTime = Math.max(latestTradeTime, maxTradeTime(trades));
			}
		}
		return f;
	}

	private long maxTradeTime(List<Trade> trades) { return trades.stream().mapToLong(Trade::getTime).max().orElse(0);	}

	private void addTrades(Fund fund, List<Trade> trades) {
		trades.stream()
			.filter(t -> t.getTime() > fund.getHoldings().get(t.getSymbol()).getLatestTradeTime())
			.forEach(t -> updateFundByTrade(fund, t));
	}

	private void updateFundByTrade(Fund fund, Trade trade) {
		if (trade.isBuyer()) {
			fund.buyStock(trade.getSymbol(), new BigDecimal(trade.getQty()), new BigDecimal(trade.getQuoteQty()));
		} else {
			updateProfit(fund, trade);
			fund.sellStock(trade.getSymbol(), new BigDecimal(trade.getQty()), new BigDecimal(trade.getQuoteQty()));
		}
		fund.getHoldings().get(trade.getSymbol()).setLatestTradeTime(trade.getTime());
	}

	private void updateProfit(Fund f, Trade trade) {
		var holding = f.getHoldings().get(trade.getSymbol());
		BigDecimal profit = new BigDecimal(trade.getPrice()).subtract(holding.getPrice()).multiply(new BigDecimal(trade.getQty()));
		f.setProfit(f.getProfit().add(profit));
	}

	private void updateProfit(Fund f, HoldingStock holding) {
		BigDecimal profit = calculateProfit(holding, f.getHoldings().get(holding.getCode()));
		f.setProfit(f.getProfit().add(profit));
	}

	private BigDecimal calculateProfit(HoldingStock holding, FundHolding fundHolding) {
		return holding.getPrice()
				.subtract(fundHolding.getPrice())
				.multiply(new BigDecimal(holding.getQuantity()));
	}
	
	
}
    
