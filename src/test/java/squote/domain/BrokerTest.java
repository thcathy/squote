package squote.domain;

import org.junit.jupiter.api.Test;
import squote.SquoteConstants;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BrokerTest {

    @Test
    public void test_IndividualFee() {
        assertEquals(new BigDecimal("15"),
                Broker.futuCommission(
                    new HoldingStock("testUser", "2800", SquoteConstants.Side.BUY, 100, BigDecimal.valueOf(30930), new Date(), null)
                )
        );

        assertEquals(new BigDecimal("50.00"),
                Broker.scbCommission(
                        new HoldingStock("testUser", "2800", SquoteConstants.Side.BUY, 100, BigDecimal.valueOf(0.1), new Date(), null))
        );
        assertEquals(new BigDecimal("61.86"),
                Broker.scbCommission(new HoldingStock("testUser", "2800", SquoteConstants.Side.BUY, 100, BigDecimal.valueOf(30930), new Date(), null))
        );
    }

}
