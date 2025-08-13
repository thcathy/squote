package squote.service;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import squote.domain.StockQuote;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TiingoAPIClientTest {
    
    private MockWebServer mockWebServer;
    private TiingoAPIClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        
        String baseUrl = mockWebServer.url("/").toString();
        client = new TiingoAPIClient(baseUrl);
        ReflectionTestUtils.setField(client, "token", "test-token");
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void getPrices_normal() throws Exception {
        String mockResponse = """
            [
              {
                "ticker": "QQQ",
                "timestamp": "2025-08-08T20:00:00+00:00",
                "open": 570.45,
                "high": 574.77,
                "low": 570.15,
                "tngoLast": 574.55,
                "last": 574.55,
                "volume": 35255472,
                "prevClose": 569.24
              },
              {
                "ticker": "SPY",
                "timestamp": "2025-08-08T20:00:00+00:00",
                "open": 550.20,
                "high": 552.30,
                "low": 549.80,
                "tngoLast": 551.75,
                "last": 551.75,
                "volume": 25000000,
                "prevClose": 550.50
              }
            ]
            """;

        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json")
                .setBody(mockResponse));

        var tickers = Arrays.asList("QQQ", "SPY");
        var quotes = client.getPrices(tickers).get();

        assertEquals(2, quotes.size());
        var qqqQuote = quotes.stream().filter(q -> "QQQ".equals(q.getStockCode())).findFirst().orElse(null);
        assertNotNull(qqqQuote);
        assertEquals("574.55", qqqQuote.getPrice());
        assertEquals("574.77", qqqQuote.getHigh());
        assertEquals("570.15", qqqQuote.getLow());
        assertEquals("5.31", qqqQuote.getChangeAmount());
        assertEquals("0.93", qqqQuote.getChange());
        assertTrue(qqqQuote.hasPrice());

        var spyQuote = quotes.stream().filter(q -> "SPY".equals(q.getStockCode())).findFirst().orElse(null);
        assertNotNull(spyQuote);
        assertEquals("551.75", spyQuote.getPrice());
        assertEquals("1.25", spyQuote.getChangeAmount());
        assertEquals("0.23", spyQuote.getChange());

        var request = mockWebServer.takeRequest();
        assertTrue(request.getPath().contains("tickers=QQQ%2CSPY"));
        assertTrue(request.getPath().contains("token=test-token"));
    }

    @Test
    void getPrices_emptyResponse() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json")
                .setBody("[]"));

        List<StockQuote> emptyResult = client.getPrices(List.of("INVALID")).get();
        assertTrue(emptyResult.isEmpty());
    }

    @Test
    void testTimezoneHandling() throws Exception {
        String mockResponse = """
            [
              {
                "ticker": "QQQ",
                "timestamp": "2025-08-11T15:30:00+00:00",
                "tngoLast": 574.55
              },
              {
                "ticker": "AAPL",
                "timestamp": "2025-08-11T12:30:00.123456-07:00",
                "tngoLast": 180.50
              },
              {
                "ticker": "SPHB",
                "timestamp": "2025-08-12T11:46:01.604694867-04:00",
                "tngoLast": 25.50
              }
            ]
            """;

        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json")
                .setBody(mockResponse));

        var quotes = client.getPrices(Arrays.asList("QQQ", "AAPL", "SPHB")).get();

        var qqqQuote = quotes.stream().filter(q -> "QQQ".equals(q.getStockCode())).findFirst().orElse(null);
        assertEquals("2025-08-11 15:30:00", qqqQuote.getLastUpdate()); // Already UTC

        StockQuote aaplQuote = quotes.stream().filter(q -> "AAPL".equals(q.getStockCode())).findFirst().orElse(null);
        assertEquals("2025-08-11 19:30:00", aaplQuote.getLastUpdate()); // PST-07:00 -> UTC+00:00

        var sphbQuote = quotes.stream().filter(q -> "SPHB".equals(q.getStockCode())).findFirst().orElse(null);
        assertEquals("2025-08-12 15:46:01", sphbQuote.getLastUpdate()); // EDT-04:00 -> UTC+00:00, nanosecond precision
    }
}
