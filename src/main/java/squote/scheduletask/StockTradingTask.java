package squote.scheduletask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.futu.openapi.FTAPI_Conn_Qot;
import com.futu.openapi.FTAPI_Conn_Trd;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import squote.domain.Market;
import squote.domain.repository.FundRepository;
import squote.service.FutuAPIClient;
import squote.service.StockTradingAlgoService;
import squote.service.TelegramAPIClient;

import java.util.Arrays;
import java.util.HashMap;
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

    public FutuAPIClientFactory futuAPIClientFactory = (futuClientConfig) -> new FutuAPIClient(futuClientConfig, new FTAPI_Conn_Trd(), new FTAPI_Conn_Qot(), rsaKey, true);

    @Autowired
    public StockTradingTask(
            FundRepository fundRepo,
            StockTradingAlgoService algoService,
            TelegramAPIClient telegramAPIClient) {
        this.fundRepo = fundRepo;
        this.algoService = algoService;
        this.telegramAPIClient = telegramAPIClient;
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

                var fundName = fund.name;
                log.info("Start process fund [{}]", fundName);
                var clientConfig = futuClientConfigs.get(fundName);
                if (clientConfig == null) {
                    log.warn("cannot find client config for fund: {}", fundName);
                    continue;
                }
                FutuAPIClient futuAPIClient = futuAPIClientFactory.build(clientConfig);

                unlockTrade(futuAPIClient, clientConfig.unlockCode());
                fund.getAlgoConfigs().values().stream()
                        .filter(c -> market.equals(Market.getMarketByStockCode(c.code())))
                        .forEach(c -> algoService.processSingleSymbol(fund, market, c, clientConfig, futuAPIClient));
                futuAPIClient.close();
            }
        } catch (Exception e) {
            log.error("Unexpected exception!" ,e);
            var message = String.format("StockTradingTask - %s: Unexpected exception: %s \n %s", market, e.getMessage(), ExceptionUtils.getStackTrace(e));
            telegramAPIClient.sendMessage(message);
        }
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
