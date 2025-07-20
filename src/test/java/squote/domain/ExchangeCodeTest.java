package squote.domain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExchangeCodeTest {

    @Test
    void testIsUSStockCode_ValidUSStockCodes() {
        Assertions.assertTrue(ExchangeCode.isUSStockCode("AAPL.XNAS"));
        Assertions.assertTrue(ExchangeCode.isUSStockCode("TSLA.XNAS"));
        Assertions.assertTrue(ExchangeCode.isUSStockCode("MSFT.XNAS"));
        Assertions.assertTrue(ExchangeCode.isUSStockCode("GOOGL.XNAS"));
    }

    @Test
    void testIsUSStockCode_NonUSStockCodes() {
        Assertions.assertFalse(ExchangeCode.isUSStockCode("2828"));
        Assertions.assertFalse(ExchangeCode.isUSStockCode("0005.HK"));
        Assertions.assertFalse(ExchangeCode.isUSStockCode("AAPL"));
        Assertions.assertFalse(ExchangeCode.isUSStockCode("TSLA.XHKG"));
        Assertions.assertFalse(ExchangeCode.isUSStockCode("BTC-USD"));
    }

    @Test
    void getBaseCodeFromTicker() {
        assertEquals("2800", ExchangeCode.getBaseCodeFromTicker("2800"));
        assertEquals("QQQ", ExchangeCode.getBaseCodeFromTicker("QQQ.XNAS"));
    }
}
