package squote.service;

import org.junit.jupiter.api.Test;
import squote.domain.Broker;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class HKEXMarketFeesCalculatorTest {

    @Test
    public void test_IndividualFee() {
        var calculator = new HKEXMarketFeesCalculator();
        assertEquals(BigDecimal.valueOf(0.5), calculator.tradingTariff());

        assertEquals(new BigDecimal("2.00"), calculator.settlementFee(BigDecimal.valueOf(0.1)));
        assertEquals(new BigDecimal("20.00"), calculator.settlementFee(BigDecimal.valueOf(1000000)));
        assertEquals(new BigDecimal("100.00").toPlainString(), calculator.settlementFee(BigDecimal.valueOf(10000000)).toPlainString());

        assertEquals(new BigDecimal("1"), calculator.stampDuty(BigDecimal.valueOf(0.1)));
        assertEquals(new BigDecimal("60"), calculator.stampDuty(BigDecimal.valueOf(45789)));

        assertEquals(new BigDecimal("0.01"), calculator.tradingFee(BigDecimal.valueOf(0.1)));
        assertEquals(new BigDecimal("1.55"), calculator.tradingFee(BigDecimal.valueOf(30930)));

        assertEquals(new BigDecimal("0.01"), calculator.transactionLevy(BigDecimal.valueOf(0.1)));
        assertEquals(new BigDecimal("0.84"), calculator.transactionLevy(BigDecimal.valueOf(30930)));
    }

    @Test
    public void test_TotalFee() {
        var calculator = new HKEXMarketFeesCalculator();
        assertEquals(BigDecimal.valueOf(19.89), calculator.totalFee(BigDecimal.valueOf(30930), false, Broker.FUTU.calculateCommission));
        assertEquals(new BigDecimal("21.30"), calculator.totalFee(BigDecimal.valueOf(49356), false, Broker.FUTU.calculateCommission));
    }
}
