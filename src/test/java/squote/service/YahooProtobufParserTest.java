package squote.service;

import org.junit.jupiter.api.Test;
import squote.service.yahoo.YahooTicker;

import static org.junit.jupiter.api.Assertions.*;

class YahooProtobufParserTest {

    @Test
    void testParseYahooTicker_ORCL() {
        String base64Data = "CgRPUkNMFc0MakMY8Ljth/plKgNOWVEwCDgBRUOG4j9IzOykAWWAPYJAsAGAAtgBBA==";
        
        YahooTicker ticker = YahooProtobufParser.parseYahooTicker(base64Data);
        
        assertNotNull(ticker, "Ticker should not be null");
        assertEquals("ORCL", ticker.getId());
        assertTrue(ticker.getPrice() > 100);
        assertEquals(1751549587000L, ticker.getTime());
        assertEquals("NYQ", ticker.getExchange());
        assertEquals(YahooTicker.QuoteType.EQUITY, ticker.getQuoteType());
        assertEquals(YahooTicker.MarketHours.REGULAR_MARKET, ticker.getMarketHours());
    }
} 
