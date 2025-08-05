package squote.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import squote.domain.AlgoConfig;
import squote.domain.Fund;
import squote.domain.StockQuote;
import squote.domain.repository.FundRepository;
import squote.service.yahoo.YahooFinanceWebSocketClient;
import squote.service.yahoo.YahooTicker;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class YahooFinanceServiceTest {
    @Mock
    private YahooFinanceWebSocketClient mockWebSocketClient;
    
    @Mock
    private FundRepository mockFundRepository;

    private YahooFinanceService yahooFinanceService;
    private Map<String, YahooTicker> latestTickers;
    private Set<String> subscribedSymbols;

    @BeforeEach
    void setUp() throws Exception {
        yahooFinanceService = new YahooFinanceService(mockFundRepository);
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

        Optional<StockQuote> result = yahooFinanceService.getLatestTicker("AAPL.US");
        StockQuote quote = result.get();
        assertEquals("AAPL.US", quote.getStockCode());
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
        Optional<StockQuote> result = yahooFinanceService.getLatestTicker("MSFT.US");
        assertFalse(result.isPresent());
    }

    @Test
    void testSubscribeToSymbols() {
        yahooFinanceService.subscribeToSymbols("AAPL.US", "MSFT.US");
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

        yahooFinanceService.unsubscribeFromSymbols("AAPL.US");

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

    @Test
    void testLoadUsMarketAlgoConfigs() {
        // Create test funds with algo configs
        Fund fundWithUsStocks = createFundWithAlgoConfigs("fund1", "userId1", 
                Arrays.asList("AAPL.US", "MSFT.US", "2800")); // Mix of US and HK stocks
        Fund fundWithHkStocks = createFundWithAlgoConfigs("fund2", "userId2", 
                Arrays.asList("0005", "2828")); // Only HK stocks
        Fund fundWithMixedStocks = createFundWithAlgoConfigs("fund3", "userId3", 
                Arrays.asList("TSLA.US", "0700")); // Mix of US and HK stocks
        when(mockFundRepository.findAll()).thenReturn(Arrays.asList(
                fundWithUsStocks, fundWithHkStocks, fundWithMixedStocks));

        yahooFinanceService.initialize();

        assertEquals(3, subscribedSymbols.size());
    }

    @Test
    void testLoadUsMarketAlgoConfigs_EmptyRepository() {
        when(mockFundRepository.findAll()).thenReturn(Arrays.asList());
        yahooFinanceService.initialize();

        assertEquals(0, subscribedSymbols.size());
    }

    private Fund createFundWithAlgoConfigs(String fundName, String userId, List<String> stockCodes) {
        Fund fund = new Fund(userId, fundName);
        fund.setType(Fund.FundType.STOCK);
        for (String stockCode : stockCodes) {
            AlgoConfig algoConfig = new AlgoConfig(stockCode, 100, null, 10, 0.8, null);
            fund.putAlgoConfig(stockCode, algoConfig);
        }
        return fund;
    }

    private void setPrivateField(String fieldName, Object value) throws Exception {
        Field field = YahooFinanceService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(yahooFinanceService, value);
    }
} 
