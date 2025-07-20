package squote.service;

import squote.domain.HoldingStock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Function;

public class HKMarketFeesCalculator {
    public static String INCLUDE_STAMP = "INCLUDE_STAMP";
    public static String EXCLUDE_STAMP = "EXCLUDE_STAMP";

    public BigDecimal totalFee(HoldingStock holding, boolean includeStampDuty, Function<HoldingStock, BigDecimal> commission) {
        var gross = holding.getGross();
        var sum = tradingTariff().add(settlementFee(gross)).add(tradingFee(gross)).add(transactionLevy(gross)).add(commission.apply(holding));
        if (includeStampDuty)
            sum = sum.add(stampDuty(gross));
        return sum;
    }

    BigDecimal tradingTariff() { return new BigDecimal("0.5"); }

    BigDecimal settlementFee(BigDecimal value) {
        value = value.multiply(new BigDecimal("0.00002"));
        value = value.min(new BigDecimal("100"));
        value = value.max(new BigDecimal("2"));
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    BigDecimal stampDuty(BigDecimal value) {
        return value.multiply(new BigDecimal("0.0013")).max(new BigDecimal("1"))
                .setScale(0, RoundingMode.HALF_UP);
    }

    BigDecimal tradingFee(BigDecimal value) {
        return value.multiply(new BigDecimal("0.00005")).max(new BigDecimal("0.01"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    BigDecimal transactionLevy(BigDecimal value) {
        return value.multiply(new BigDecimal("0.000027")).max(new BigDecimal("0.01"))
                .setScale(2, RoundingMode.HALF_UP);
    }

}
