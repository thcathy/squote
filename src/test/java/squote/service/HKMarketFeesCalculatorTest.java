package squote.service;

import org.junit.jupiter.api.Test;
import squote.SquoteConstants;
import squote.domain.Broker;
import squote.domain.HoldingStock;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HKMarketFeesCalculatorTest {

    @Test
    public void test_IndividualFee() {
        var calculator = new HKMarketFeesCalculator();
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
        var calculator = new HKMarketFeesCalculator();
        assertEquals(BigDecimal.valueOf(19.89), calculator.totalFee(
                new HoldingStock("testUser", "2800", SquoteConstants.Side.BUY, 100, BigDecimal.valueOf(30930), new Date(), null),
                false, Broker.FUTU.calculateCommission));
        assertEquals(new BigDecimal("21.30"), calculator.totalFee(
                new HoldingStock("testUser", "2800", SquoteConstants.Side.BUY, 100, BigDecimal.valueOf(49356), new Date(), null),
                false, Broker.FUTU.calculateCommission));
    }
}
