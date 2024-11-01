package squote.scheduletask;

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
import squote.domain.HoldingStock;
import squote.domain.repository.FundRepository;
import squote.domain.repository.HoldingStockRepository;
import squote.service.EmailService;
import squote.service.FutuAPIClient;
import squote.service.HKEXMarketFeesCalculator;
import squote.service.UpdateFundByHoldingService;

import java.util.Calendar;
import java.util.Date;

@Component
public class SyncStockExecutionsTask {
    record ClientConfig(String ip, short port, String fundName, long accountId) {
    }

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Value(value = "${syncstockexecutionstask.enabled}") boolean enabled;
    @Value(value = "${syncstockexecutionstask.clientConfigsJson}") String clientConfigJson;
    @Value(value = "${syncstockexecutionstask.userId}") String userId;
    @Value(value = "${syncstockexecutionstask.rsakey}") String rsaKey;

    @Autowired HoldingStockRepository holdingRepo;
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
        StringBuilder logs = new StringBuilder("Start SyncStockExecutionsTask\n");
        FutuAPIClient futuAPIClient = null;
        try {
            var clientConfigs = mapper.readValue(clientConfigJson, ClientConfig[].class);
            for (var config : clientConfigs) {
                logs.append("---------------------------\n");
                logs.append("Process config=").append(config).append("\n");
                futuAPIClient = futuAPIClientFactory.build(config.ip, config.port);

                var maxHolding = holdingRepo.findTopByFundNameOrderByDateDesc(config.fundName);
                var fromDate = maxHolding.isPresent() ? maxHolding.get().getDate() : getLastMonth();
                logs.append("Get executions for accountId=").append(config.accountId).append(" since ").append(fromDate).append("\n");

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
                    logs.append("updated fund profit=").append(fund.getProfit()).append("\n");
                }
                futuAPIClient.close();
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

    private Date getLastMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        return calendar.getTime();
    }
}
