package squote.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import squote.domain.StockQuote;
import squote.service.yahoo.YahooFinanceWebSocketClient;
import squote.service.yahoo.YahooTicker;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class YahooFinanceServiceTest {
    @Mock
    private YahooFinanceWebSocketClient mockWebSocketClient;

    private YahooFinanceService yahooFinanceService;
    private Map<String, YahooTicker> latestTickers;
    private Set<String> subscribedSymbols;

    @BeforeEach
    void setUp() throws Exception {
        yahooFinanceService = new YahooFinanceService();
        latestTickers = new ConcurrentHashMap<>();
        subscribedSymbols = new CopyOnWriteArraySet<>();
        setPrivateField("webSocketClient", mockWebSocketClient);
        setPrivateField("latestTickers", latestTickers);
        setPrivateField("subscribedSymbols", subscribedSymbols);
    }

    @Test
    void testGetLatestTicker_SymbolExists() {
        YahooTicker ticker = createSampleTicker("AAPL", 150.75f);
        ticker.setDayHigh(155.0f);
        ticker.setDayLow(149.0f);
        ticker.setChangePercent(1.5f);
        ticker.setChange(2.25f);
        ticker.setTime(1704460200000L);
        latestTickers.put("AAPL", ticker);

        Optional<StockQuote> result = yahooFinanceService.getLatestTicker("AAPL.XNAS");
        StockQuote quote = result.get();
        assertEquals("AAPL.XNAS", quote.getStockCode());
        assertEquals("Apple Inc", quote.getStockName());
        assertEquals("150.750", quote.getPrice());
        assertEquals("155.000", quote.getHigh());
        assertEquals("149.000", quote.getLow());
        assertEquals("1.500", quote.getChange());
        assertEquals("2.250", quote.getChangeAmount());
        assertEquals("2024-01-05 21:10", quote.getLastUpdate());
    }

    @Test
    void testGetLatestTicker_SymbolNotExists() {
        Optional<StockQuote> result = yahooFinanceService.getLatestTicker("MSFT.XNAS");
        assertFalse(result.isPresent());
    }

    @Test
    void testSubscribeToSymbols() {
        yahooFinanceService.subscribeToSymbols("AAPL.XNAS", "MSFT.XNAS");
        verify(mockWebSocketClient).subscribeToSymbols("AAPL", "MSFT");
        assertTrue(subscribedSymbols.contains("AAPL"));
        assertTrue(subscribedSymbols.contains("MSFT"));
    }

    @Test
    void testSubscribeToSymbols_EmptyArray() {
        yahooFinanceService.subscribeToSymbols();
        verifyNoInteractions(mockWebSocketClient);
    }

    @Test
    void testUnsubscribeFromSymbols() {
        subscribedSymbols.add("AAPL");
        subscribedSymbols.add("MSFT");
        latestTickers.put("AAPL", createSampleTicker("AAPL", 150.0f));
        latestTickers.put("MSFT", createSampleTicker("MSFT", 300.0f));

        yahooFinanceService.unsubscribeFromSymbols("AAPL.XNAS");

        assertFalse(subscribedSymbols.contains("AAPL"));
        assertFalse(latestTickers.containsKey("AAPL"));
        assertTrue(subscribedSymbols.contains("MSFT"));
        assertTrue(latestTickers.containsKey("MSFT"));
    }

    private YahooTicker createSampleTicker(String id, float price) {
        YahooTicker ticker = new YahooTicker();
        ticker.setId(id);
        ticker.setPrice(price);
        ticker.setShortName(id.equals("AAPL") ? "Apple Inc" : id.equals("MSFT") ? "Microsoft Corp" : "Tesla Inc");
        ticker.setTime(System.currentTimeMillis());
        return ticker;
    }

    private void setPrivateField(String fieldName, Object value) throws Exception {
        Field field = YahooFinanceService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(yahooFinanceService, value);
    }
} 
