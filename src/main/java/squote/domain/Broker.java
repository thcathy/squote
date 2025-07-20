package squote.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Function;

public enum Broker {
    FUTU("", Broker::futuCommission),
    SCBANK("", Broker::scbCommission),
    MOX("", Broker::moxCommission),
    USMART("", Broker::defaultCommission);

    public final String chineseName;
    public final Function<HoldingStock, BigDecimal> calculateCommission;

    Broker(String chineseName, Function<HoldingStock, BigDecimal> calculateCommission) {
        this.calculateCommission = calculateCommission;
        this.chineseName = chineseName;
    }

    static BigDecimal futuCommission(HoldingStock holding) {
        var market = ExchangeCode.getMarketByStockCode(holding.getCode());
        return switch (market) {
            case HK -> new BigDecimal("15");
            case US -> {
                // Commission: $0.0049/Share, minimum $0.99/order
                BigDecimal commission = BigDecimal.valueOf(holding.getQuantity() * 0.0049)
                        .max(new BigDecimal("0.99"))
                        .setScale(2, RoundingMode.HALF_UP);
                
                // Platform fees: $0.005/Share, minimum $1/order  
                BigDecimal platformFees = BigDecimal.valueOf(holding.getQuantity() * 0.005)
                        .max(new BigDecimal("1.00"))
                        .setScale(2, RoundingMode.HALF_UP);
                
                yield commission.add(platformFees);
            }
        };
    }

    static BigDecimal moxCommission(HoldingStock holding) {
        return holding.getGross().multiply(new BigDecimal("0.0012"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal scbCommission(HoldingStock holding) {
        return holding.getGross().multiply(new BigDecimal("0.002")).max(new BigDecimal("50"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal defaultCommission(HoldingStock holding) {
        return BigDecimal.ZERO;
    }
}
