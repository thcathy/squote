package squote.service;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import squote.SquoteConstants.Side;
import squote.domain.Fund;
import squote.domain.FundHolding;
import squote.domain.HoldingStock;
import squote.domain.repository.FundRepository;
import squote.domain.repository.HoldingStockRepository;

public class UpdateFundByHoldingService {
	protected final Logger log = LoggerFactory.getLogger(getClass());
		
	@Autowired FundRepository fundRepo;
	@Autowired HoldingStockRepository holdingRepo;
	
		
	public UpdateFundByHoldingService(FundRepository fundRepo, HoldingStockRepository holdingRepo) {
		this.fundRepo = fundRepo;
		this.holdingRepo = holdingRepo;
	}


	public Fund updateFundByHolding(String fundName, BigInteger holdingId) {
		Fund f = fundRepo.findOne(fundName);
		HoldingStock holding = holdingRepo.findOne(holdingId);
		
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
    
