package squote.scheduletask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.futu.openapi.FTAPI_Conn_Qot;
import com.futu.openapi.FTAPI_Conn_Trd;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import squote.domain.AlgoConfig;
import squote.domain.Market;
import squote.domain.StockQuote;
import squote.domain.repository.FundRepository;
import squote.service.FutuAPIClient;
import squote.service.StockTradingAlgoService;
import squote.service.TelegramAPIClient;
import squote.service.TiingoAPIClient;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class StockTradingTask {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Value("#{${stocktradingtask.enabled}}") public Map<String, Boolean> enabledByMarket;
    @Value(value = "${futuOpendRsaKey}") String rsaKey;
    @Value(value = "${futuClientConfigsJson}") String clientConfigJson;

    final FundRepository fundRepo;
    private final StockTradingAlgoService algoService;
    final TelegramAPIClient telegramAPIClient;
    final TiingoAPIClient tiingoAPIClient;

    public FutuAPIClientFactory futuAPIClientFactory = (futuClientConfig) -> new FutuAPIClient(futuClientConfig, new FTAPI_Conn_Trd(), new FTAPI_Conn_Qot(), rsaKey, true);

    @Autowired
    public StockTradingTask(
            FundRepository fundRepo,
            StockTradingAlgoService algoService,
            TelegramAPIClient telegramAPIClient,
            TiingoAPIClient tiingoAPIClient) {
        this.fundRepo = fundRepo;
        this.algoService = algoService;
        this.telegramAPIClient = telegramAPIClient;
        this.tiingoAPIClient = tiingoAPIClient;
    }

    private boolean isMarketDisabled(Market market) {
        return !enabledByMarket.getOrDefault(market.toString(), false);
    }
    
    @Scheduled(cron = "0 30-55/5 9 * * MON-FRI", zone = "Asia/Hong_Kong")
    @Scheduled(cron = "0 */5 10-15 * * MON-FRI", zone = "Asia/Hong_Kong")
    @Scheduled(cron = "0 5 18 * * MON-FRI", zone = "Asia/Hong_Kong")    // adjust the price after daily std dev calculated
    public void executeHK() {
        if (isMarketDisabled(Market.HK)) { log.info("HK trading task disabled");
            return;
        }

        innerExecute(Market.HK);
    }

    @PostConstruct
    public void init() {
        innerExecute(Market.US);
    }

    @Scheduled(cron = "0 */5 4-19 * * MON-FRI", zone = "America/New_York")
    public void executeUS() {
        if (isMarketDisabled(Market.US)) {
            log.info("US trading task disabled");
            return;
        }

        innerExecute(Market.US);
    }

    public void innerExecute(Market market) {
        try {
            log.info("Starting stock trading task for market: {}", market);
            var futuClientConfigs = parseFutuClientConfigs();

            for (var fund : fundRepo.findAll()) {
                if (fund.getAlgoConfigs().isEmpty()) continue;

                var algoConfigsMatchMarket = fund.getAlgoConfigs().values().stream()
                        .filter(c -> market.equals(Market.getMarketByStockCode(c.code())))
                        .toList();
                if (algoConfigsMatchMarket.isEmpty()) continue;

                var fundName = fund.name;
                log.info("Start process fund [{}]", fundName);
                var clientConfig = futuClientConfigs.get(fundName);
                if (clientConfig == null) {
                    log.warn("cannot find client config for fund: {}", fundName);
                    continue;
                }
                FutuAPIClient futuAPIClient = futuAPIClientFactory.build(clientConfig);
                unlockTrade(futuAPIClient, clientConfig.unlockCode());

                var usQuotes = getUSStockQuote(algoConfigsMatchMarket, fundName);
                algoConfigsMatchMarket.forEach(c -> algoService.processSingleSymbol(fund, market, c, clientConfig, futuAPIClient, usQuotes.get(Market.getBaseCodeFromTicker(c.code()))));
                futuAPIClient.close();
            }
        } catch (Exception e) {
            log.error("Unexpected exception!" ,e);
            var message = String.format("StockTradingTask - %s: Unexpected exception: %s \n %s", market, e.getMessage(), ExceptionUtils.getStackTrace(e));
            telegramAPIClient.sendMessage(message);
        }
    }

    @NotNull
    private Map<String, StockQuote> getUSStockQuote(List<AlgoConfig> algoConfigsMatchMarket, String fundName) {
        var usStockCodes = algoConfigsMatchMarket.stream()
                .map(AlgoConfig::code)
                .filter(Market::isUSStockCode)
                .map(Market::getBaseCodeFromTicker)
                .distinct().toList();

        var usQuotes = new HashMap<String, StockQuote>();

        if (usStockCodes.isEmpty()) return usQuotes;

        try {
            var quotes = tiingoAPIClient.getPrices(usStockCodes).get();
            quotes.forEach(quote -> usQuotes.put(quote.getStockCode(), quote));
            log.info("Fetched {} quotes from Tiingo for fund {}: {}", quotes.size(), fundName, usStockCodes);
        } catch (Exception e) {
            log.warn("Failed to fetch quotes from Tiingo for fund {}. Exception={}", fundName, e.getMessage());
        }
        return usQuotes;
    }

    public void unlockTrade(FutuAPIClient futuAPIClient, String code) {
        if (!futuAPIClient.unlockTrade(code)) {
            throw new RuntimeException("unlock trade failed");
        }
    }

    private Map<String, FutuClientConfig> parseFutuClientConfigs() {
        try
        {
            var mapper = new ObjectMapper();
            var clientConfigs = mapper.readValue(clientConfigJson, FutuClientConfig[].class);
            return Arrays.stream(clientConfigs).collect(Collectors.toMap(FutuClientConfig::fundName, o -> o));
        } catch (Exception e) {
            log.error("Cannot parse config: {}", clientConfigJson ,e);
        }
        return new HashMap<>();
    }
}
