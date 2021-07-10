package squote.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BrokerTest {

    @Test
    public void test_IndividualFee() {
        assertEquals(new BigDecimal("15"), Broker.futuCommission(BigDecimal.valueOf(30930)));

        assertEquals(new BigDecimal("50.00"), Broker.scbCommission(BigDecimal.valueOf(0.1)));
        assertEquals(new BigDecimal("61.86"), Broker.scbCommission(BigDecimal.valueOf(30930)));
    }

}
