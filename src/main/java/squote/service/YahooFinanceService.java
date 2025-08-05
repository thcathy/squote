package squote.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import squote.domain.AlgoConfig;
import squote.domain.Market;
import squote.domain.StockQuote;
import squote.domain.repository.FundRepository;
import squote.service.yahoo.YahooFinanceWebSocketClient;
import squote.service.yahoo.YahooTicker;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

@Service
public class YahooFinanceService {

    private static final Logger log = LoggerFactory.getLogger(YahooFinanceService.class);
    private final FundRepository fundRepository;
    private YahooFinanceWebSocketClient webSocketClient;
    private final Map<String, YahooTicker> latestTickers = new ConcurrentHashMap<>();
    private final Set<String> subscribedSymbols = new CopyOnWriteArraySet<>();

    public YahooFinanceService(FundRepository fundRepository) {
        this.fundRepository = fundRepository;
    }

    @PostConstruct
    public void initialize() {
        createAndConnectClient();
        loadUsMarketAlgoConfigs();
        log.info("YahooFinanceService initialized");
    }

    private void loadUsMarketAlgoConfigs() {
        try {
            var usMarketAlgoConfigs = StreamSupport.stream(fundRepository.findAll().spliterator(), false)
                    .flatMap(fund -> fund.getAlgoConfigs().values().stream())
                    .filter(algoConfig -> Market.US.equals(Market.getMarketByStockCode(algoConfig.code())))
                    .toList();
            
            log.info("Found {} US market algo configs from all funds", usMarketAlgoConfigs.size());
            if (!usMarketAlgoConfigs.isEmpty()) {
                var symbols = usMarketAlgoConfigs.stream()
                        .map(AlgoConfig::code).distinct().toArray(String[]::new);
                subscribeToSymbols(symbols);
            }
        } catch (Exception e) {
            log.error("Failed to load US market algo configs", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (webSocketClient != null) {
            webSocketClient.close();
            log.info("YahooFinanceService shutdown");
        }
    }

    public void subscribeToSymbols(String... codeWithMarketCode) {
        if (codeWithMarketCode.length == 0) return;

        var codesWithoutSuffix = Arrays.stream(codeWithMarketCode).map(YahooFinanceService::getPrefix).toList();
        log.info("subscribeToSymbols: {}", String.join(",", codesWithoutSuffix));
        subscribedSymbols.addAll(codesWithoutSuffix);
        webSocketClient.subscribeToSymbols(codesWithoutSuffix.toArray(new String[0]));
    }

    public void unsubscribeFromSymbols(String... codeWithMarketCode) {
        var codesWithoutSuffix = Arrays.stream(codeWithMarketCode).map(YahooFinanceService::getPrefix).toList();
        log.info("unsubscribeFromSymbols: {}", String.join(",", codesWithoutSuffix));
        for (var code : codesWithoutSuffix) {
            subscribedSymbols.remove(code);
            latestTickers.remove(code);
        }
    }

    private static String getPrefix(String code) {
        return code.split("\\.")[0];
    }

    public Optional<StockQuote> getLatestTicker(String symbol) {
        var code = getPrefix(symbol);
        if (!latestTickers.containsKey(code)) return Optional.empty();

        YahooTicker ticker = latestTickers.get(code);
        return Optional.of(convertToStockQuote(ticker, symbol));
    }

    private StockQuote convertToStockQuote(YahooTicker ticker, String originalSymbol) {
        StockQuote stockQuote = new StockQuote(originalSymbol);
        
        stockQuote.setStockName(ticker.getShortName() != null ? ticker.getShortName() : "");
        stockQuote.setPrice(formatFloatValue(ticker.getPrice()));
        stockQuote.setHigh(formatFloatValue(ticker.getDayHigh()));
        stockQuote.setLow(formatFloatValue(ticker.getDayLow()));
        stockQuote.setChange(formatFloatValue(ticker.getChangePercent()));
        stockQuote.setChangeAmount(formatFloatValue(ticker.getChange()));
        stockQuote.setLastUpdate(formatTimestampToHKT(ticker.getTime()));
        
        return stockQuote;
    }
    
    private String formatFloatValue(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value) || value == 0.0f) {
            return "NA";
        }
        return String.format("%.3f", value);
    }
    
    private String formatTimestampToHKT(long timestamp) {
        try {
            Instant instant = Instant.ofEpochMilli(timestamp);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneId.of("Asia/Hong_Kong"));
            return formatter.format(instant);
        } catch (Exception e) {
            log.warn("Error formatting timestamp {} to HKT: {}", timestamp, e.getMessage());
            return String.valueOf(timestamp);
        }
    }

    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    public void checkConnectionHealth() {
        try {
            if (webSocketClient == null || !webSocketClient.isOpen()) {
                log.warn("WebSocket connection is down, attempting to reconnect...");
                reconnect();
            } else {
                log.debug("WebSocket connection is healthy");
            }
        } catch (Exception e) {
            log.error("Error during connection health check", e);
        }
    }

    /**
     * Force reconnection to Yahoo Finance WebSocket
     */
    public synchronized void reconnect() {
        try {
            if (webSocketClient != null) webSocketClient.close();
            createAndConnectClient();

            if (webSocketClient.waitForConnection(10, TimeUnit.SECONDS)) {
                if (!subscribedSymbols.isEmpty()) {
                    String[] symbolsArray = subscribedSymbols.toArray(new String[0]);
                    webSocketClient.subscribeToSymbols(symbolsArray);
                }
            }
        } catch (Exception e) {
            log.error("Error during reconnection", e);
        }
    }

    private void createAndConnectClient() {
        webSocketClient = new YahooFinanceWebSocketClient();
        webSocketClient.addTickerCallback(this::onTickerUpdate);
        webSocketClient.connect();
        log.info("Connecting to Yahoo Finance WebSocket...");
        webSocketClient.waitForConnection(10, TimeUnit.SECONDS);
    }

    private void onTickerUpdate(YahooTicker ticker) {
        String symbol = ticker.getId();
        log.debug("Updated ticker for {}: price=${}", symbol, ticker.getPrice());
        latestTickers.put(symbol, ticker);
    }
}
