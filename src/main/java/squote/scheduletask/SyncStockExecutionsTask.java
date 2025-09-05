package squote.scheduletask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.futu.openapi.FTAPI_Conn_Qot;
import com.futu.openapi.FTAPI_Conn_Trd;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import squote.domain.*;
import squote.domain.repository.FundRepository;
import squote.domain.repository.HoldingStockRepository;
import squote.domain.repository.TaskConfigRepository;
import squote.service.*;

import java.math.BigDecimal;
import java.util.*;

@Component
public class SyncStockExecutionsTask {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Value("#{${syncstockexecutionstask.enabled}}") public Map<String, Boolean> enabledByMarket;
    @Value(value = "${syncstockexecutionstask.userId}") String userId;
    @Value(value = "${futuOpendRsaKey}") String rsaKey;
    @Value(value = "${futuClientConfigsJson}") String clientConfigJson;
    @Value(value = "${syncstockexecutionstask.summaryEmailAddress}") String summaryEmailAddress;
    @Value(value = "${syncstockexecutionstask.sendTelegram}") boolean sendTelegram;

    @Autowired HoldingStockRepository holdingRepo;
    @Autowired TaskConfigRepository taskConfigRepo;
    @Autowired FundRepository fundRepo;
    @Autowired UpdateFundByHoldingService updateFundService;
    @Autowired EmailService emailService;
    @Autowired TelegramAPIClient telegramAPIClient;

    HKMarketFeesCalculator hkFeeCalculator = new HKMarketFeesCalculator();
    USMarketFeesCalculator usFeeCalculator = new USMarketFeesCalculator();

    public FutuAPIClientFactory futuAPIClientFactory = (futuClientConfig) -> new FutuAPIClient(futuClientConfig, new FTAPI_Conn_Trd(), new FTAPI_Conn_Qot(), rsaKey, true);

    @Scheduled(cron = "0 5 17 * * MON-SAT", zone = "Asia/Hong_Kong")
    public void executeHK() {
        if (isMarketDisabled(Market.HK) || StringUtils.isEmpty(clientConfigJson)) {
            log.info("Task Disabled");
            return;
        }

        try {
            sync(Market.HK);
        } catch (Exception e) {
            var message = String.format("SyncStockExecutionsTask: Unexpected exception: %s \n %s", e.getMessage(), ExceptionUtils.getStackTrace(e));
            log.error("HK market execution failed", e);
            sendTelegram(message);
        }
    }

    @Scheduled(cron = "0 0 0 * * TUE-SAT", zone = "America/New_York")
    public void executeUS() {
        if (isMarketDisabled(Market.US) || StringUtils.isEmpty(clientConfigJson)) {
            log.info("US Task Disabled");
            return;
        }

        try {
            sync(Market.US);
        } catch (Exception e) {
            var message = String.format("SyncStockExecutionsTask US: Unexpected exception: %s \n %s", e.getMessage(), ExceptionUtils.getStackTrace(e));
            log.error("US market execution failed", e);
            sendTelegram(message);
        }
    }

    public void sync(Market market) {
        var mapper = new ObjectMapper();
        StringBuilder logs = new StringBuilder("Start SyncStockExecutionsTask for market: " + market + "\n\n");
        FutuAPIClient futuAPIClient = null;
        try {
            var futuClientConfigs = mapper.readValue(clientConfigJson, FutuClientConfig[].class);
            var fromDate = getFromDate(market);

            for (var config : futuClientConfigs) {
                if (config.markets()==null || !config.markets().contains(market)) continue;

                logs.append(String.format("Process config=%s\n\nFund snapshot before:\n%s\n\n", config, fundRepo.findByUserIdAndName(userId, config.fundName())));
                futuAPIClient = futuAPIClientFactory.build(config);

                logs.append(String.format("Get executions for accountId=%s since %s\n\n", config.accountId(), fromDate));
                var executions = futuAPIClient.getStockExecutions(fromDate, market);
                for (var exec : executions.values()) {
                    logs.append(String.format("\nProcess execution=%s\n", exec));
                    if (exec.getMarket() != market) {
                        logs.append(String.format("%s!=%s Skip processing\n", exec.getMarket(), market));
                        continue;
                    }

                    if (holdingRepo.existsByFillIdsLike(exec.getFillIds())) {
                        logs.append("Fill id exists. Skip processing\n");
                        continue;
                    }

                    var holding = HoldingStock.from(exec, userId);
                    holding = holdingRepo.save(holding);
                    var stockMarket = Market.getMarketByStockCode(holding.getCode());
                    BigDecimal fees = (stockMarket == Market.US) 
                        ? usFeeCalculator.totalFee(holding, Broker.FUTU.calculateCommission)
                        : hkFeeCalculator.totalFee(holding, false, Broker.FUTU.calculateCommission);
                    holding.setFee(fees);
                    var fund = updateFundService.updateFundByHolding(userId, config.fundName(), holding, fees);
                    fundRepo.save(fund);
                    holdingRepo.save(holding);

                    logHoldingProcessed(config, logs, holding, fees, fund);
                }
                futuAPIClient.close();

                saveLastExecutionTime(logs, market, fromDate, executions);
                logs.append(String.format("Fund snapshot after:\n%s\n\n", fundRepo.findByUserIdAndName(userId, config.fundName())));
            }
        } catch (Exception e) {
            var message = String.format("SyncStockExecutionsTask: Unexpected exception: %s \n %s", e.getMessage(), ExceptionUtils.getStackTrace(e));
            logs.append(String.format("ERROR, stop execute\n\n%s", message));
            sendTelegram(message);
        } finally {
            if (futuAPIClient != null) futuAPIClient.close();

            var logsString = logs.toString();
            log.info(logsString);
            sendSummaryEmail(logsString);
        }
    }

    private void logHoldingProcessed(FutuClientConfig config, StringBuilder logs, HoldingStock holding, BigDecimal fees, Fund fund) {
        logs.append(String.format("created holding=%s\n", holding));
        logs.append(String.format("update with holding to fund %s:%s with fee %s\n", 
                userId, config.fundName(), fees));
        logs.append(String.format("updated fund profit=%s\n\n", fund.getProfit()));

        var message = String.format("""
Created holding for %s
%s %s %d@%.2f (%.2f)
fee=%.2f profit=%.2f""",
                fund.name,
                holding.getSide(), holding.getCode(), holding.getQuantity(), holding.getPrice(), holding.getGross(),
                fees, fund.getProfit());
        sendTelegram(message);
    }

    private void sendTelegram(String message) {
        if (sendTelegram) {
            telegramAPIClient.sendMessage(message);
        }
    }

    private void sendSummaryEmail(String logsString) {
        if (StringUtils.isNotBlank(summaryEmailAddress))
            emailService.sendEmail(summaryEmailAddress, "SyncStockExecutionsTask Executed", logsString);
    }

    private void saveLastExecutionTime(StringBuilder logs, Market market, Date fromDate, HashMap<String, Execution> executions) {
        var marketExecutions = executions.values().stream()
                .filter(exec -> exec.getMarket() == market)
                .toList();
        
        if (marketExecutions.isEmpty()) {
            log.info("Do not update last execution time when no executions proceed for market {}", market);
            return;
        }

        long maxTime = marketExecutions.stream()
                .mapToLong(Execution::getTime)
                .max().orElseThrow();
        var date = new Date(Math.max(maxTime, fromDate.getTime()));

        var taskConfig = taskConfigRepo.findById(this.getClass().toString())
                .orElseGet(() -> new TaskConfig(this.getClass().toString(), ""));
        var config = taskConfig.jsonConfig().isEmpty()
                ? new SyncStockExecutionsTaskConfig(new HashMap<>())
                : SyncStockExecutionsTaskConfig.fromJson(taskConfig.jsonConfig());
        var maxDate = new Date(Math.max(config.lastExecutionTimeByMarket().getOrDefault(market, fromDate).getTime(), date.getTime()));
        config.lastExecutionTimeByMarket().put(market, maxDate);
        taskConfigRepo.save(new TaskConfig(this.getClass().toString(), SyncStockExecutionsTaskConfig.toJson(config)));
        log.info("Saved last execution time for market {}: {}", market, date);
        logs.append(String.format("Saved last execution time for market %s: %s\n\n", market, date));
    }

    private Date getFromDate(Market market) {
        var date = getLastExecutionTime(market).orElseGet(this::getLastMonth);
        return new Date(date.getTime() + 1000); // 1 second
    }

    private Optional<Date> getLastExecutionTime(Market market) {
        try {
            var entity = taskConfigRepo.findById(this.getClass().toString()).orElseThrow();
            var config = SyncStockExecutionsTaskConfig.fromJson(entity.jsonConfig());
            if (config.lastExecutionTimeByMarket() == null || !config.lastExecutionTimeByMarket().containsKey(market))
                return Optional.empty();

            var time = config.lastExecutionTimeByMarket().get(market);
            log.info("lastExecutionTime in config: {}", time);
            return Optional.ofNullable(time); // 1 hour
        } catch (Exception e) {
            log.info("Cannot get last execution time for {}: {}", market, e.toString());
            return Optional.empty();
        }
    }

    private Date getLastMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        return calendar.getTime();
    }

    private boolean isMarketDisabled(Market market) {
        return !enabledByMarket.getOrDefault(market.toString(), false);
    }
}
