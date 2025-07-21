package squote.service;

import squote.SquoteConstants;
import squote.domain.HoldingStock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Function;

public class USMarketFeesCalculator {
    
    public BigDecimal totalFee(HoldingStock holding, Function<HoldingStock, BigDecimal> commission) {
        return finraTAF(holding).add(settlementFee(holding)).add(commission.apply(holding));
    }

    BigDecimal finraTAF(HoldingStock holding) {
        if (holding.getSide() != SquoteConstants.Side.SELL) return BigDecimal.ZERO;
        
        // FINRA Trading Activity Fee: $0.000166 per share
        return BigDecimal.valueOf(holding.getQuantity())
                .multiply(new BigDecimal("0.000166"))
                .setScale(2, RoundingMode.HALF_UP);
    }



    BigDecimal settlementFee(HoldingStock holding) {
        // NSCC and DTC settlement fees: $0.00030 per share
        BigDecimal shares = new BigDecimal(holding.getQuantity());
        return shares.multiply(new BigDecimal("0.00030")).setScale(2, RoundingMode.HALF_UP);
    }

} 
