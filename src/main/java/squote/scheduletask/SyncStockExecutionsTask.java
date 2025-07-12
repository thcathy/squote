package squote.scheduletask;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Component
public class SyncStockExecutionsTask {
    record SyncStockExecutionsTaskConfig(Map<ExchangeCode.Market, Date> lastExecutionTimeByMarket) {
        static String toJson(SyncStockExecutionsTaskConfig config) {
            try {
                return new ObjectMapper().writeValueAsString(config);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        static SyncStockExecutionsTaskConfig fromJson(String json) {
            try {
                return new ObjectMapper().readValue(json, SyncStockExecutionsTaskConfig.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

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

    HKEXMarketFeesCalculator feeCalculator = new HKEXMarketFeesCalculator();

    public FutuAPIClientFactory futuAPIClientFactory = (futuClientConfig) -> new FutuAPIClient(futuClientConfig, new FTAPI_Conn_Trd(), new FTAPI_Conn_Qot(), rsaKey, true);

    @Scheduled(cron = "0 5 17 * * MON-SAT", zone = "Asia/Hong_Kong")
    public void executeHK() {
        if (isMarketDisabled(ExchangeCode.Market.HK) || StringUtils.isEmpty(clientConfigJson)) {
            log.info("Task Disabled");
            return;
        }

        try {
            sync(ExchangeCode.Market.HK);
        } catch (Exception e) {
            var message = String.format("SyncStockExecutionsTask: Unexpected exception: %s \n %s", e.getMessage(), ExceptionUtils.getStackTrace(e));
            log.error("HK market execution failed", e);
            sendTelegram(message);
        }
    }

    @Scheduled(cron = "0 5 00 * * TUE-SAT", zone = "America/New_York")
    public void executeUS() {
        if (isMarketDisabled(ExchangeCode.Market.US) || StringUtils.isEmpty(clientConfigJson)) {
            log.info("US Task Disabled");
            return;
        }

        try {
            sync(ExchangeCode.Market.US);
        } catch (Exception e) {
            var message = String.format("SyncStockExecutionsTask US: Unexpected exception: %s \n %s", e.getMessage(), ExceptionUtils.getStackTrace(e));
            log.error("US market execution failed", e);
            sendTelegram(message);
        }
    }

    public void sync(ExchangeCode.Market market) {
        var mapper = new ObjectMapper();
        StringBuilder logs = new StringBuilder("Start SyncStockExecutionsTask for market: " + market + "\n\n");
        FutuAPIClient futuAPIClient = null;
        try {
            var futuClientConfigs = mapper.readValue(clientConfigJson, FutuClientConfig[].class);
            var fromDate = getFromDate(market);

            for (var config : futuClientConfigs) {
                if (config.markets() ==null || !config.markets().contains(market)) continue;

                logs.append("Process config=").append(config).append("\n\n");
                logs.append("Fund snapshot before:\n").append(fundRepo.findByUserIdAndName(userId, config.fundName())).append("\n\n");
                futuAPIClient = futuAPIClientFactory.build(config);

                logs.append("Get executions for accountId=").append(config.accountId()).append(" since ").append(fromDate).append("\n\n");
                var executions = futuAPIClient.getStockExecutions(fromDate, market);
                for (var exec : executions.values()) {
                    logs.append("\n").append("Process execution=").append(exec).append("\n");
                    if (exec.getMarket() != market) {
                        logs.append(exec.getMarket()).append("!=").append(market).append(" Skip processing\n");
                        continue;
                    }

                    if (holdingRepo.existsByFillIdsLike(exec.getFillIds())) {
                        logs.append("Fill id exists. Skip processing\n");
                        continue;
                    }

                    var holding = HoldingStock.from(exec, userId);
                    holding = holdingRepo.save(holding);
                    var fees = feeCalculator.totalFee(holding.getGross(), false, Broker.FUTU.calculateCommission);
                    var fund = updateFundService.updateFundByHolding(userId, config.fundName(), holding, fees);
                    fundRepo.save(fund);
                    holdingRepo.save(holding);

                    logHoldingProcessed(config, logs, holding, fees, fund);
                }
                futuAPIClient.close();

                saveLastExecutionTime(logs, market, fromDate, executions);
                logs.append("Fund snapshot after:\n").append(fundRepo.findByUserIdAndName(userId, config.fundName())).append("\n\n");
            }
        } catch (Exception e) {
            var message = String.format("SyncStockExecutionsTask: Unexpected exception: %s \n %s", e.getMessage(), ExceptionUtils.getStackTrace(e));
            logs.append("ERROR, stop execute\n\n").append(message);
            sendTelegram(message);
        } finally {
            if (futuAPIClient != null) futuAPIClient.close();

            var logsString = logs.toString();
            log.info(logsString);
            sendSummaryEmail(logsString);
        }
    }

    private void logHoldingProcessed(FutuClientConfig config, StringBuilder logs, HoldingStock holding, BigDecimal fees, Fund fund) {
        logs.append("created holding=").append(holding).append("\n");
        logs.append("update with holding to fund ")
                .append(userId).append(":").append(config.fundName())
                .append(" with fee ").append(fees).append("\n");
        logs.append("updated fund profit=").append(fund.getProfit()).append("\n\n");

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

    private void saveLastExecutionTime(StringBuilder logs, ExchangeCode.Market market, Date fromDate, HashMap<String, Execution> executions) {
        if (executions.isEmpty()) {
            log.info("Do not update last execution time when no executions proceed");
            return;
        }

        long maxTime = executions.values().stream()
                .mapToLong(Execution::getTime)
                .max().orElseThrow();
        var date = new Date(Math.max(maxTime, fromDate.getTime()));

        var taskConfig = taskConfigRepo.findById(this.getClass().toString()).orElseGet(() -> new TaskConfig(this.getClass().toString(), ""));
        var config = taskConfig.jsonConfig().isEmpty() ? new SyncStockExecutionsTaskConfig(new HashMap<>()) : SyncStockExecutionsTaskConfig.fromJson(taskConfig.jsonConfig());
        config.lastExecutionTimeByMarket.put(market, date);
        taskConfigRepo.save(new TaskConfig(this.getClass().toString(), SyncStockExecutionsTaskConfig.toJson(config)));
        log.info("Saved last execution time: {}", date);
        logs.append("Saved last execution time: ").append(date).append("\n\n");
    }

    private Date getFromDate(ExchangeCode.Market market) {
        var date = getLastExecutionTime(market).orElseGet(this::getLastMonth);
        return getTomorrowAtMidnight(date);
    }

    private Optional<Date> getLastExecutionTime(ExchangeCode.Market market) {
        try {
            var entity = taskConfigRepo.findById(this.getClass().toString()).orElseThrow();
            var config = SyncStockExecutionsTaskConfig.fromJson(entity.jsonConfig());
            if (config.lastExecutionTimeByMarket == null || !config.lastExecutionTimeByMarket.containsKey(market))
                return Optional.empty();

            var time = config.lastExecutionTimeByMarket.get(market);
            log.info("lastExecutionTime in config: {}", time);
            return Optional.ofNullable(time); // 1 hour
        } catch (Exception e) {
            log.info("Cannot get last execution time for {}: {}", market, e.toString());
            return Optional.empty();
        }
    }

    public static Date getTomorrowAtMidnight(Date date) {
        ZonedDateTime zonedDateTime = date.toInstant().atZone(ZoneId.systemDefault());
        ZonedDateTime tomorrowMidnight = zonedDateTime.plusDays(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        return Date.from(tomorrowMidnight.toInstant());
    }

    private Date getLastMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        return calendar.getTime();
    }

    private boolean isMarketDisabled(ExchangeCode.Market market) {
        return !enabledByMarket.getOrDefault(market.toString(), false);
    }
}
