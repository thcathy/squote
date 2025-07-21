package squote.domain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarketTest {

    @Test
    void testIsUSStockCode_ValidUSStockCodes() {
        Assertions.assertTrue(Market.isUSStockCode("AAPL.US"));
        Assertions.assertTrue(Market.isUSStockCode("TSLA.US"));
        Assertions.assertTrue(Market.isUSStockCode("MSFT.US"));
        Assertions.assertTrue(Market.isUSStockCode("GOOGL.US"));
    }

    @Test
    void testIsUSStockCode_NonUSStockCodes() {
        Assertions.assertFalse(Market.isUSStockCode("2828"));
        Assertions.assertFalse(Market.isUSStockCode("BTC-USD"));
    }

    @Test
    void getBaseCodeFromTicker() {
        assertEquals("2800", Market.getBaseCodeFromTicker("2800"));
        assertEquals("QQQ", Market.getBaseCodeFromTicker("QQQ.US"));
    }
}
