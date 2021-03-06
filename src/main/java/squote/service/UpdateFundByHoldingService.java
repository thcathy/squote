package squote.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import squote.SquoteConstants.Side;
import squote.domain.Execution;
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
	
	public Fund updateFundByHoldingAndPersist(String userId, String fundName, String holdingId, BigDecimal fee) {
		Fund f = updateFundByHolding(userId, fundName, holdingId, fee);
		fundRepo.save(f);
		return f;
	}

	public Fund updateFundByHolding(String userId, String fundName, String holdingId, BigDecimal fee) {
		Fund f = fundRepo.findByUserIdAndName(userId, fundName).get();
		HoldingStock holding = holdingRepo.findById(holdingId).get();
		
		if (Side.BUY.equals(holding.getSide()))
			f.buyStock(holding.getCode(), BigDecimal.valueOf(holding.getQuantity()), holding.getGross());
		else if (Side.SELL.equals(holding.getSide())) {
			updateProfit(f, holding);			
			f.sellStock(holding.getCode(), BigDecimal.valueOf(holding.getQuantity()), holding.getGross());
		}
		holding.setFundName(f.name);
		f.setProfit(f.getProfit().subtract(fee));
		return f;
	}

	public Fund getTradesAndUpdateFund(String userId, String fundName, String source) {
		Fund f = fundRepo.findByUserIdAndName(userId, fundName).get();
		if (SOURCE_BINANCE.equalsIgnoreCase(source)) {
			for (String code : f.getHoldings().keySet()) {
				var executions = binanceAPIService.getMyTrades(code);
				var holding = f.getHoldings().get(code);
				addExecutionsToFund(f, executions);
				holding.setLatestTradeTime(Math.max(holding.getLatestTradeTime(), maxTime(executions)));
			}
		}
		fundRepo.save(f);
		return f;
	}

	private long maxTime(List<Execution> executions) { return executions.stream().mapToLong(Execution::getTime).max().orElse(0);	}

	private void addExecutionsToFund(Fund fund, List<Execution> executions) {
		executions.stream()
			.filter(exec -> exec.getTime() > fund.getHoldings().get(exec.getSymbol()).getLatestTradeTime())
			.forEach(exec -> updateFundByExecution(fund, exec));
	}

	private void updateFundByExecution(Fund fund, Execution exec) {
		if (Side.BUY == exec.getSide()) {
			fund.buyStock(exec.getSymbol(), exec.getQuantity(), exec.getQuoteQuantity());
		} else {
			updateProfit(fund, exec);
			fund.sellStock(exec.getSymbol(), exec.getQuantity(), exec.getQuoteQuantity());
		}
	}

	private void updateProfit(Fund f, Execution exec) {
		var holding = f.getHoldings().get(exec.getSymbol());
		BigDecimal profit = exec.getPrice().subtract(holding.getPrice()).multiply(exec.getQuantity());
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
    
