package squote.service;

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

public class UpdateFundByHoldingService {
	protected final Logger log = LoggerFactory.getLogger(getClass());
		
	@Autowired FundRepository fundRepo;
	@Autowired HoldingStockRepository holdingRepo;
	
		
	public UpdateFundByHoldingService(FundRepository fundRepo, HoldingStockRepository holdingRepo) {
		this.fundRepo = fundRepo;
		this.holdingRepo = holdingRepo;
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
			f.buyStock(holding.getCode(), holding.getQuantity(), holding.getGross());
		else if (Side.SELL.equals(holding.getSide())) {
			updateProfit(f, holding);			
			f.sellStock(holding.getCode(), holding.getQuantity(), holding.getGross());			
		}
		
		return f;
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
    
