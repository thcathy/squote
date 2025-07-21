package squote.service;

import org.junit.jupiter.api.Test;
import squote.SquoteConstants.Side;
import squote.domain.Broker;
import squote.domain.HoldingStock;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class USMarketFeesCalculatorTest {

    @Test
    public void test_TotalFee_WithActualFUTUBroker() {
        var calculator = new USMarketFeesCalculator();
        var buyHolding = createTestHolding("QQQ.US", Side.BUY, 500, new BigDecimal("125000"));
        assertEquals(new BigDecimal("5.10"), calculator.totalFee(buyHolding, Broker.FUTU.calculateCommission));

        var sellHolding = createTestHolding("QQQ.US", Side.SELL, 500, new BigDecimal("125000"));
        assertEquals(new BigDecimal("5.18"), calculator.totalFee(sellHolding, Broker.FUTU.calculateCommission));
    }

    private HoldingStock createTestHolding(String code, Side side, int quantity, BigDecimal gross) {
        return new HoldingStock("testUser", code, side, quantity, gross, new Date(), null);
    }
} 
