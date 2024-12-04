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
import squote.domain.Broker;
import squote.domain.Execution;
import squote.domain.HoldingStock;
import squote.domain.TaskConfig;
import squote.domain.repository.FundRepository;
import squote.domain.repository.HoldingStockRepository;
import squote.domain.repository.TaskConfigRepository;
import squote.service.EmailService;
import squote.service.FutuAPIClient;
import squote.service.HKEXMarketFeesCalculator;
import squote.service.UpdateFundByHoldingService;

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

    record ClientConfig(String ip, short port, String fundName, long accountId) {}

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Value(value = "${syncstockexecutionstask.enabled}") boolean enabled;
    @Value(value = "${syncstockexecutionstask.clientConfigsJson}") String clientConfigJson;
    @Value(value = "${syncstockexecutionstask.userId}") String userId;
    @Value(value = "${syncstockexecutionstask.rsakey}") String rsaKey;

    @Autowired HoldingStockRepository holdingRepo;
    @Autowired TaskConfigRepository taskConfigRepo;
    @Autowired FundRepository fundRepo;
    @Autowired UpdateFundByHoldingService updateFundService;
    @Autowired EmailService emailService;

    HKEXMarketFeesCalculator feeCalculator = new HKEXMarketFeesCalculator();

    public FutuAPIClientFactory futuAPIClientFactory = (ip, port) -> new FutuAPIClient(new FTAPI_Conn_Trd(), ip, port, rsaKey, true);

    @Scheduled(cron = "0 30 16 * * MON-FRI", zone = "Asia/Hong_Kong")
    public void executeTask() {
        if (!enabled || StringUtils.isEmpty(clientConfigJson)) {
            log.info("Task Disabled");
            return;
        }

        var mapper = new ObjectMapper();
        StringBuilder logs = new StringBuilder("Start SyncStockExecutionsTask\n\n");
        FutuAPIClient futuAPIClient = null;
        try {
            var clientConfigs = mapper.readValue(clientConfigJson, ClientConfig[].class);
            var fromDate = getFromDate();

            for (var config : clientConfigs) {
                logs.append("Process config=").append(config).append("\n\n");
                logs.append("Fund snapshot before:\n").append(fundRepo.findByUserIdAndName(userId, config.fundName)).append("\n\n");
                futuAPIClient = futuAPIClientFactory.build(config.ip, config.port);

                logs.append("Get executions for accountId=").append(config.accountId).append(" since ").append(fromDate).append("\n\n");
                var executions = futuAPIClient.getHKStockExecutions(config.accountId, fromDate);
                for (var exec : executions.values()) {
                    logs.append("\n").append("Process execution=").append(exec).append("\n");
                    if (holdingRepo.existsByFillIdsLike(exec.getFillIds())) {
                        logs.append("Fill id exists. Skip processing\n");
                        continue;
                    }

                    var holding = HoldingStock.from(exec, userId);
                    holding = holdingRepo.save(holding);
                    var fees = feeCalculator.totalFee(holding.getGross(), false, Broker.FUTU.calculateCommission);
                    var fund = updateFundService.updateFundByHolding(userId, config.fundName, holding, fees);
                    fundRepo.save(fund);
                    holdingRepo.save(holding);

                    logs.append("created holding=").append(holding).append("\n");
                    logs.append("update with holding to fund ")
                            .append(userId).append(":").append(config.fundName)
                            .append(" with fee ").append(fees).append("\n");
                    logs.append("updated fund profit=").append(fund.getProfit()).append("\n\n");
                }
                futuAPIClient.close();

                var updatedDate = saveLastExecutionTime(fromDate, executions);
                log.info("Saved last execution time: {}", updatedDate);
                logs.append("Saved last execution time: ").append(updatedDate).append("\n\n");
                logs.append("Fund snapshot after:\n").append(fundRepo.findByUserIdAndName(userId, config.fundName)).append("\n\n");
            }
        } catch (Exception e) {
            logs.append("ERROR, stop execute\n\n").append(ExceptionUtils.getStackTrace(e));
        } finally {
            if (futuAPIClient != null) futuAPIClient.close();

            var logsString = logs.toString();
            log.info(logsString);
            emailService.sendEmail("thcathy@gmail.com", "SyncStockExecutionsTask Executed", logsString);
        }
    }

    private Date saveLastExecutionTime(Date fromDate, HashMap<String, Execution> executions) {
        if (executions.isEmpty()) return fromDate;

        long maxTime = executions.values().stream()
                .mapToLong(Execution::getTime)
                .max().orElseThrow();
        var date = new Date(Math.max(maxTime, fromDate.getTime()));
        var jsonConfig = SyncStockExecutionsTaskConfig.toJson(new SyncStockExecutionsTaskConfig(date));
        taskConfigRepo.save(new TaskConfig(this.getClass().toString(), jsonConfig));
        return fromDate;
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
