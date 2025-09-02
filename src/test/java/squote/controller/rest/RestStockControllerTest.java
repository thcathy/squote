package squote.controller.rest;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import squote.IntegrationTest;
import squote.SquoteConstants;
import squote.domain.Fund;
import squote.domain.HoldingStock;
import squote.domain.StockQuote;
import squote.domain.repository.FundRepository;
import squote.domain.repository.HoldingStockRepository;
import squote.security.AuthenticationServiceStub;
import squote.service.BinanceAPIService;
import squote.service.YahooFinanceService;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.StreamSupport;

import static java.math.RoundingMode.HALF_UP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class RestStockControllerTest extends IntegrationTest {
    @Autowired RestStockController restStockController;
    @Autowired HoldingStockRepository holdingStockRepository;
    @Autowired FundRepository fundRepository;

    @Autowired AuthenticationServiceStub authenticationServiceStub;

    BinanceAPIService mockBinanceAPIService;
    BinanceAPIService realBinanceAPIService;
    YahooFinanceService mockYahooFinanceService;
    YahooFinanceService realYahooFinanceService;

    @BeforeEach
    public void setup() {
        realBinanceAPIService = restStockController.binanceAPIService;
        mockBinanceAPIService = Mockito.mock(BinanceAPIService.class);
        restStockController.binanceAPIService = mockBinanceAPIService;
        
        realYahooFinanceService = restStockController.yahooFinanceService;
        mockYahooFinanceService = Mockito.mock(YahooFinanceService.class);
        restStockController.yahooFinanceService = mockYahooFinanceService;
    }

    @AfterEach
    public void resetStub() {
        authenticationServiceStub.userId = authenticationServiceStub.TESTER_USERID;
        restStockController.binanceAPIService = realBinanceAPIService;
        restStockController.yahooFinanceService = realYahooFinanceService;
    }

    @Test
    public void collectAllStockQuotes_willRemoveDuplicate() {
        StockQuote[] duplicateStockQuotes = new StockQuote[]{new StockQuote("2800"), new StockQuote("2800")};
        Map<String, StockQuote> result = restStockController.collectAllStockQuotes(duplicateStockQuotes);

        assertThat(result.size()).isEqualTo(1);
    }

    @Test
    public void testStockQuery_saveAndLoad() {
        String result = restStockController.loadQuery();
        assertThat(result).isEqualTo("");

        String codes = "123,321,123456";
        result = restStockController.saveQuery(codes);
        assertThat(result).isEqualTo(codes);

        result = restStockController.loadQuery();
        assertThat(result).isEqualTo(codes);

        authenticationServiceStub.userId = "another user id";
        result = restStockController.loadQuery();
        assertThat(result).isEqualTo("");
    }

    @Test
    public void test_listHolding() {
        Iterable<HoldingStock> holdings = restStockController.listHolding();
        long countBeforeStart = StreamSupport.stream(holdings.spliterator(), false).count();

        holdingStockRepository.save(createSell2800Holding(authenticationServiceStub.TESTER_USERID));

        holdings = restStockController.listHolding();
        long count = StreamSupport.stream(holdings.spliterator(), false).count();
        assertThat(count).isEqualTo(countBeforeStart + 1);

        authenticationServiceStub.userId = "another user id";
        holdings = restStockController.listHolding();
        count = StreamSupport.stream(holdings.spliterator(), false).count();
        assertThat(count).isEqualTo(0);
    }

    @Test
    public void test_deleteHolding() {
        authenticationServiceStub.userId = UUID.randomUUID().toString();
        HoldingStock holding = holdingStockRepository.save(createSell2800Holding(authenticationServiceStub.userId));

        Iterable<HoldingStock> holdings = restStockController.listHolding();
        long count = StreamSupport.stream(holdings.spliterator(), false).count();
        assertThat(count).isEqualTo(1);

        holdings = restStockController.deleteHolding(holding.getId());
        count = StreamSupport.stream(holdings.spliterator(), false).count();
        assertThat(count).isEqualTo(0);
    }

    @Test
    public void test_deleteHoldingPair() {
        authenticationServiceStub.userId = UUID.randomUUID().toString();
        HoldingStock buyHolding = holdingStockRepository.save(createSell2800Holding(authenticationServiceStub.userId));
        HoldingStock sellHolding = holdingStockRepository.save(createBuy2800Holding(authenticationServiceStub.userId));

        Iterable<HoldingStock> holdings = restStockController.listHolding();
        long count = StreamSupport.stream(holdings.spliterator(), false).count();
        assertThat(count).isEqualTo(2);

        holdings = restStockController.deleteHoldingPair(sellHolding.getId(), buyHolding.getId());
        count = StreamSupport.stream(holdings.spliterator(), false).count();
        assertThat(count).isEqualTo(0);
    }

    @Test
    public void test_quote_supportMultiUser() throws ExecutionException, InterruptedException {
        when(mockYahooFinanceService.getLatestTicker(Mockito.anyString())).thenReturn(Optional.empty());
        
        authenticationServiceStub.userId = UUID.randomUUID().toString();
        holdingStockRepository.save(createSell2800Holding(authenticationServiceStub.userId));

        Map<String, Object> resultMap = restStockController.quote("");
        assertThat(((List)resultMap.get("holdings")).size()).isEqualTo(1);
        assertThat(((List)resultMap.get("funds")).size()).isEqualTo(0);

        authenticationServiceStub.userId = authenticationServiceStub.TESTER_USERID;
        resultMap = restStockController.quote("");
        assertThat(((List)resultMap.get("holdings")).size()).isEqualTo(0);
        assertThat(((List)resultMap.get("funds")).size()).isEqualTo(0);
    }

    @Test
    public void quote_givenCryptoFund_willUpdateNetProfit() throws Exception {
        when(mockYahooFinanceService.getLatestTicker(Mockito.anyString())).thenReturn(Optional.empty());
        
        authenticationServiceStub.userId = UUID.randomUUID().toString();
        Fund testFund = createCryptoFund(authenticationServiceStub.userId);
        fundRepository.save(testFund);
        var quote = new StockQuote("BTCUSDT").setPrice("36000");
        when(mockBinanceAPIService.getAllPrices()).thenReturn(Map.of("BTCUSDT", quote));

        Map<String, Object> resultMap = restStockController.quote("");
        List<Fund> funds = (List<Fund>) resultMap.get("funds");
        assertThat(((Map)resultMap.get("allQuotes")).containsKey("BTCUSDT")).isTrue();
        assertThat(funds.get(0).getNetProfit().setScale(0, HALF_UP)).isEqualTo(BigDecimal.valueOf(100));
        assertThat(funds.get(0).getHoldings().get("BTCUSDT").getNetProfit().setScale(0, HALF_UP)).isEqualTo(BigDecimal.valueOf(100));

        fundRepository.delete(testFund);
    }

    @Test
    public void test_quote_WithUSStocks() throws ExecutionException, InterruptedException {
        authenticationServiceStub.userId = UUID.randomUUID().toString();
        String codes = "AAPL.US,2800";
        StockQuote aaplQuote = new StockQuote("AAPL.US").setPrice("150.75");
        
        when(mockYahooFinanceService.getLatestTicker("AAPL.US")).thenReturn(Optional.of(aaplQuote));
        when(mockBinanceAPIService.getAllPrices()).thenReturn(Collections.emptyMap());
        
        Map<String, Object> resultMap = restStockController.quote(codes);
        Map<String, StockQuote> allQuotes = (Map<String, StockQuote>) resultMap.get("allQuotes");
        
        assertThat(allQuotes).containsKey("AAPL.US");
        assertThat(allQuotes.get("AAPL.US").getPrice()).isEqualTo("150.75");
        Mockito.verify(mockYahooFinanceService).subscribeToSymbols("AAPL.US");
    }

    @Test
    public void test_quote_missingUSStockQuote_shouldNotContainNullInQuotes() throws ExecutionException, InterruptedException {
        when(mockYahooFinanceService.getLatestTicker("QQQ.US")).thenReturn(Optional.empty());
        when(mockBinanceAPIService.getAllPrices()).thenReturn(Collections.emptyMap());
        
        Map<String, Object> resultMap = restStockController.quote("QQQ.US");
        List<StockQuote> quotes = (List<StockQuote>) resultMap.get("quotes");
        assertThat(quotes).doesNotContainNull();
    }

    private HoldingStock createSell2800Holding(String userId) {
        var holding = new HoldingStock(userId, "2800", SquoteConstants.Side.SELL, 2000, new BigDecimal("40400"), new Date(), null);
        holding.setFee(BigDecimal.valueOf(25));
        return holding;
    }

    private HoldingStock createBuy2800Holding(String userId) {
        var holding = new HoldingStock(userId, "2800", SquoteConstants.Side.BUY, 2000, new BigDecimal("40000"), new Date(), null);
        holding.setFee(BigDecimal.valueOf(20));
        return holding;
    }

    private Fund createCryptoFund(String userId) {
        Fund fund = new Fund(userId, this.getClass().getSimpleName() + "test");
        fund.setType(Fund.FundType.CRYPTO);
        fund.buyStock("BTCUSDT", BigDecimal.valueOf(0.1), BigDecimal.valueOf(3500));
        return fund;
    }
}
