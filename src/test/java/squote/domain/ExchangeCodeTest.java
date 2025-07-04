package squote.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExchangeCodeTest {

    @Test
    void testIsUSStockCode_ValidUSStockCodes() {
        assertTrue(ExchangeCode.isUSStockCode("AAPL.XNAS"));
        assertTrue(ExchangeCode.isUSStockCode("TSLA.XNAS"));
        assertTrue(ExchangeCode.isUSStockCode("MSFT.XNAS"));
        assertTrue(ExchangeCode.isUSStockCode("GOOGL.XNAS"));
    }

    @Test
    void testIsUSStockCode_NonUSStockCodes() {
        assertFalse(ExchangeCode.isUSStockCode("2828"));
        assertFalse(ExchangeCode.isUSStockCode("0005.HK"));
        assertFalse(ExchangeCode.isUSStockCode("AAPL"));
        assertFalse(ExchangeCode.isUSStockCode("TSLA.XHKG"));
        assertFalse(ExchangeCode.isUSStockCode("BTC-USD"));
    }
} 
