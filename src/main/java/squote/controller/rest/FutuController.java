package squote.controller.rest;

import com.futu.openapi.FTAPI_Conn_Qot;
import com.futu.openapi.FTAPI_Conn_Trd;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import squote.domain.FlowSummaryInfo;
import squote.domain.Market;
import squote.scheduletask.FutuClientConfig;
import squote.security.AuthenticationService;
import squote.service.FutuAPIClient;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

@RequestMapping("/rest/futu")
@RestController
public class FutuController {
    private static final Logger log = LoggerFactory.getLogger(FutuController.class);

    @Value("${futuOpendRsaKey}") String rsaKey;
    @Value("${futuClientConfigsJson}") String clientConfigJson;
    @Value("${futu.api.rateLimitDelayMs:1500}") int getFlowSummaryDelayMs;
    Map<String, FutuClientConfig> futuClientConfigs;

    @Autowired 
    AuthenticationService authenticationService;

    @PostConstruct
    public void init() {
        futuClientConfigs = FutuClientConfig.parseFutuClientConfigs(clientConfigJson);
    }

    public FutuAPIClientFactory futuAPIClientFactory = (futuClientConfig) -> 
        new FutuAPIClient(futuClientConfig, new FTAPI_Conn_Trd(), new FTAPI_Conn_Qot(), rsaKey, true);

    @GetMapping(value = "/{accountId}/{market}/{fromDate}/{toDate}")
    public List<FlowSummaryInfo> getFlowSummary(@PathVariable long accountId, @PathVariable String market,
                                                @PathVariable String fromDate, @PathVariable String toDate)
            throws ParseException {
        var userId = authenticationService.getUserId().get();
        log.info("get flow summary for market {} on date {} - {} for account {}", market, fromDate, toDate, accountId);

        var futuClientConfig = futuClientConfigs.values().stream()
                .filter(config -> config.accountId() == accountId && config.fundUserId().equals(userId))
                .findFirst().orElseThrow();
        var futuAPIClient = futuAPIClientFactory.build(futuClientConfig);
        var marketEnum = Market.valueOf(market.toUpperCase());

        var dateFormat = new SimpleDateFormat("yyyyMMdd");
        var fromDateParsed = dateFormat.parse(fromDate);
        var toDateParsed = dateFormat.parse(toDate);
        var allFlowSummary = new ArrayList<FlowSummaryInfo>();

        var calendar = Calendar.getInstance();
        calendar.setTime(fromDateParsed);

        try {
            while (!calendar.getTime().after(toDateParsed)) {
                var currentDate = calendar.getTime();
                var dailyFlowSummary = futuAPIClient.getFlowSummary(currentDate, marketEnum);
                allFlowSummary.addAll(dailyFlowSummary);
                log.info("Retrieved {} flow summary entries for market {} on date {}",
                        dailyFlowSummary.size(), market, dateFormat.format(currentDate));

                calendar.add(Calendar.DAY_OF_MONTH, 1);
                Thread.sleep(getFlowSummaryDelayMs); // rate limited in Futu Opend
            }
        } catch (Exception e) {
            log.error("Error retrieving flow summary", e);
        } finally {
            futuAPIClient.close();
        }

        return allFlowSummary;
    }

    @FunctionalInterface
    public interface FutuAPIClientFactory {
        FutuAPIClient build(FutuClientConfig futuClientConfig);
    }
}
