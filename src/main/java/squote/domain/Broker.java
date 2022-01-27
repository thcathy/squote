package squote.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Function;

public enum Broker {
    FUTU("", Broker::futuCommission),
    SCBANK("", Broker::scbCommission),
    USMART("", Broker::defaultCommission);

    public String chineseName;
    public Function<BigDecimal, BigDecimal> calculateCommission;

    Broker(String chineseName, Function<BigDecimal, BigDecimal> calculatCommission) {
        this.calculateCommission = calculatCommission;
        this.chineseName = chineseName;
    }

    static BigDecimal futuCommission(BigDecimal value) {
        return new BigDecimal("15");
    }

    static BigDecimal scbCommission(BigDecimal value) {
        return value.multiply(new BigDecimal("0.002")).max(new BigDecimal("50"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal defaultCommission(BigDecimal value) {
        return BigDecimal.ZERO;
    }
}
