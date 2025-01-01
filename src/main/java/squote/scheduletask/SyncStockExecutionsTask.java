package squote.scheduletask;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;

@Component
public class SyncStockExecutionsTask {
    record SyncStockExecutionsTaskConfig(Date lastExecutionTime) {
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

    @Value(value = "${syncstockexecutionstask.enabled}") boolean enabled;
    @Value(value = "${syncstockexecutionstask.userId}") String userId;
    @Value(value = "${futuOpendRsaKey}") String rsaKey;
    @Value(value = "${futuClientConfigsJson}") String clientConfigJson;
    @Value(value = "${syncstockexecutionstask.summaryEmailAddress}") String summaryEmailAddress;

    @Autowired HoldingStockRepository holdingRepo;
    @Autowired TaskConfigRepository taskConfigRepo;
    @Autowired FundRepository fundRepo;
    @Autowired UpdateFundByHoldingService updateFundService;
    @Autowired EmailService emailService;
    @Autowired TelegramAPIClient telegramAPIClient;

    HKEXMarketFeesCalculator feeCalculator = new HKEXMarketFeesCalculator();

    public FutuAPIClientFactory futuAPIClientFactory = (ip, port) -> new FutuAPIClient(new FTAPI_Conn_Trd(), ip, port, rsaKey, true);

    @Scheduled(cron = "0 30 16 * * MON-SAT", zone = "Asia/Hong_Kong")
    public void executeTask() {
        if (!enabled || StringUtils.isEmpty(clientConfigJson)) {
            log.info("Task Disabled");
            return;
        }

        var mapper = new ObjectMapper();
        StringBuilder logs = new StringBuilder("Start SyncStockExecutionsTask\n\n");
        FutuAPIClient futuAPIClient = null;
        try {
            var clientConfigs = mapper.readValue(clientConfigJson, FutuClientConfig[].class);
            var fromDate = getFromDate();

            for (var config : clientConfigs) {
                logs.append("Process config=").append(config).append("\n\n");
                logs.append("Fund snapshot before:\n").append(fundRepo.findByUserIdAndName(userId, config.fundName())).append("\n\n");
                futuAPIClient = futuAPIClientFactory.build(config.ip(), config.port());

                logs.append("Get executions for accountId=").append(config.accountId()).append(" since ").append(fromDate).append("\n\n");
                var executions = futuAPIClient.getHKStockExecutions(config.accountId(), fromDate);
                for (var exec : executions.values()) {
                    logs.append("\n").append("Process execution=").append(exec).append("\n");
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

                saveLastExecutionTime(logs, fromDate, executions);
                logs.append("Fund snapshot after:\n").append(fundRepo.findByUserIdAndName(userId, config.fundName())).append("\n\n");
            }
        } catch (Exception e) {
            var message = String.format("SyncStockExecutionsTask: Unexpected exception: %s \n %s", e.getMessage(), ExceptionUtils.getStackTrace(e));
            logs.append("ERROR, stop execute\n\n").append(message);
            telegramAPIClient.sendMessage(message);
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

        var messaage = String.format("""
Created holding: %s %s %d@%.2f (%.2f)
Updated fund: %s, fee=%.2f, profit=%.2f""",
                holding.getSide(), holding.getCode(), holding.getQuantity(),
                holding.getPrice(), holding.getGross(),
                fund.name, fees, fund.getProfit());
        telegramAPIClient.sendMessage(messaage);
    }

    private void sendSummaryEmail(String logsString) {
        if (StringUtils.isNotBlank(summaryEmailAddress))
            emailService.sendEmail(summaryEmailAddress, "SyncStockExecutionsTask Executed", logsString);
    }

    private void saveLastExecutionTime(StringBuilder logs, Date fromDate, HashMap<String, Execution> executions) {
        if (executions.isEmpty()) {
            log.info("Do not update last execution time when no executions proceed");
            return;
        }

        long maxTime = executions.values().stream()
                .mapToLong(Execution::getTime)
                .max().orElseThrow();
        var date = new Date(Math.max(maxTime, fromDate.getTime()));
        var jsonConfig = SyncStockExecutionsTaskConfig.toJson(new SyncStockExecutionsTaskConfig(date));
        taskConfigRepo.save(new TaskConfig(this.getClass().toString(), jsonConfig));
        log.info("Saved last execution time: {}", date);
        logs.append("Saved last execution time: ").append(date).append("\n\n");
    }

    private Date getFromDate() {
        var date = getLastExecutionTime().orElseGet(this::getLastMonth);
        return getTomorrowAtMidnight(date);
    }

    private Optional<Date> getLastExecutionTime() {
        try {
            var entity = taskConfigRepo.findById(this.getClass().toString()).orElseThrow();
            var lastExecution = SyncStockExecutionsTaskConfig.fromJson(entity.jsonConfig()).lastExecutionTime;
            log.info("lastExecutionTime in config: {}", lastExecution);
            return Optional.ofNullable(lastExecution); // 1 hour
        } catch (Exception e) {
            log.info("Cannot get last execution time: {}", e.toString());
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
}
