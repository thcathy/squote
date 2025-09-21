package squote.scheduletask;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import squote.IntegrationTest;
import squote.domain.Execution;
import squote.domain.Fund;
import squote.domain.Market;
import squote.domain.TaskConfig;
import squote.domain.repository.FundRepository;
import squote.domain.repository.HoldingStockRepository;
import squote.domain.repository.TaskConfigRepository;
import squote.service.EmailService;
import squote.service.FutuAPIClient;
import squote.service.TelegramAPIClient;
import squote.service.UpdateFundByHoldingService;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static squote.SquoteConstants.Side.BUY;

@AutoConfigureMockMvc
class SyncStockExecutionsTaskIntegrationTest extends IntegrationTest {
    @Autowired SyncStockExecutionsTask task;
    @Autowired FundRepository fundRepo;
    @Autowired HoldingStockRepository holdingRepo;
    @Autowired TaskConfigRepository taskConfigRepo;
    @Autowired UpdateFundByHoldingService updateFundByHoldingService;
    // @Autowired EmailService emailService;    // for actual SMTP testing

    FutuAPIClientFactory mockFactory = Mockito.mock(FutuAPIClientFactory.class);
    FutuAPIClient mockFutuAPIClient = Mockito.mock(FutuAPIClient.class);
    EmailService mockEmailService = Mockito.mock(EmailService.class);
    TelegramAPIClient mockTelegramAPIClient = Mockito.mock(TelegramAPIClient.class);


    @BeforeEach
    void setup() {
        holdingRepo.deleteAll();
        fundRepo.deleteAll();
        when(mockFactory.build(any())).thenReturn(mockFutuAPIClient);
        when(mockTelegramAPIClient.sendMessage(any(String.class))).thenReturn(List.of("Message sent"));

        task = new SyncStockExecutionsTask();
        task.holdingRepo = holdingRepo;
        task.fundRepo = fundRepo;
        task.emailService = mockEmailService;
        task.telegramAPIClient = mockTelegramAPIClient;
        task.sendTelegram = true;
        task.enabledByMarket = Map.of("HK", true);
        task.futuAPIClientFactory = mockFactory;
        task.updateFundService = updateFundByHoldingService;
        task.taskConfigRepo = taskConfigRepo;
        task.clientConfigJson = """
        [
            {"ip":"127.0.0.1","port":1,"fundUserId":"userA","fundName":"A","accountId":1234567,"unlockCode":"","markets":["HK"]}
        ]""";

        taskConfigRepo.deleteAll();
    }

    @Test
    void executeTaskTest() {
        var userId = UUID.randomUUID().toString();
        task.userId = userId;
        Fund f = new Fund(userId, "A");
        f.buyStock("2800", BigDecimal.valueOf(1000), BigDecimal.valueOf(25000));
        fundRepo.save(f);

        var executions = new HashMap<String, Execution>();
        var exec = new Execution();
        var execDate = new Date();
        exec.setOrderId("orderId1");
        exec.setFillIds(",fillId1");
        exec.setQuantity(BigDecimal.valueOf(1000));
        exec.setPrice(BigDecimal.valueOf(25));
        exec.setSide(BUY);
        exec.setCode("2800");
        exec.setTime(execDate.getTime());
        exec.setMarket(Market.HK);
        executions.put(exec.getOrderId(), exec);

        // Add US execution with newer timestamp to test market filtering
        var usExec = new Execution();
        var usExecDate = new Date(execDate.getTime() + 3600000); // 1 hour later
        usExec.setOrderId("usOrderId1");
        usExec.setFillIds(",usFillId1");
        usExec.setQuantity(BigDecimal.valueOf(200));
        usExec.setPrice(BigDecimal.valueOf(30));
        usExec.setSide(BUY);
        usExec.setCode("AAPL");
        usExec.setTime(usExecDate.getTime());
        usExec.setMarket(Market.US);
        executions.put(usExec.getOrderId(), usExec);

        when(mockFutuAPIClient.getRecentExecutions(any(Date.class), eq(Market.HK))).thenReturn(executions);
        task.executeHK();

        var holdings = Lists.newArrayList(holdingRepo.findAll());
        assertEquals(1, holdings.size());
        var holding = holdings.getFirst();
        assertEquals("2800", holding.getCode());
        assertEquals(userId, holding.getUserId());
        assertEquals("A", holding.getFundName());
        assertEquals(1000, holding.getQuantity());
        assertEquals(execDate, holding.getDate());
        assertEquals(19.43, holding.getFee().doubleValue());
        verify(mockTelegramAPIClient, times(1)).sendMessage(startsWith("Created holding for A"));

        var fund = fundRepo.findByUserIdAndName(userId, "A").get();
        assertEquals(-19.43, fund.getProfit().doubleValue());   // due to fee
        var fundHolding = fund.getHoldings().get("2800");
        assertEquals(2000, fundHolding.getQuantity().intValue());
        assertEquals(50000, fundHolding.getGross().intValue());

        var configEntity = taskConfigRepo.findById(SyncStockExecutionsTask.class.toString()).orElseThrow();
        var config = SyncStockExecutionsTaskConfig.fromJson(configEntity.jsonConfig());
        assertEquals(execDate, config.lastExecutionTimeByMarket().get(Market.HK));

        // run the task again will not repeat same fill id
        task.executeHK();
        fund = fundRepo.findByUserIdAndName(userId, "A").get();
        assertEquals(-19.43, fund.getProfit().doubleValue());   // due to fee
        fundHolding = fund.getHoldings().get("2800");
        assertEquals(2000, fundHolding.getQuantity().intValue());
        assertEquals(50000, fundHolding.getGross().intValue());
    }

    @Test
    void executeTask_onlySaveMaxExecutionTime() {
        var userId = UUID.randomUUID().toString();
        task.userId = userId;
        Fund f = new Fund(userId, "A");
        fundRepo.save(f);

        var configDate = new Date();
        var jsonConfig = SyncStockExecutionsTaskConfig.toJson(
                new SyncStockExecutionsTaskConfig(Map.of(Market.HK, configDate)));
        taskConfigRepo.save(new TaskConfig(SyncStockExecutionsTask.class.toString(), jsonConfig));

        var executions = new HashMap<String, Execution>();
        var exec = new Execution();
        var execDate = new Date(configDate.getTime() - 86400000);   // 1 day ago
        exec.setOrderId("orderId1");
        exec.setFillIds(",fillId1");
        exec.setQuantity(BigDecimal.valueOf(1000));
        exec.setPrice(BigDecimal.valueOf(25));
        exec.setSide(BUY);
        exec.setCode("2800");
        exec.setTime(execDate.getTime());
        exec.setMarket(Market.HK);
        executions.put(exec.getOrderId(), exec);
        when(mockFutuAPIClient.getRecentExecutions(any(Date.class), eq(Market.HK))).thenReturn(executions);

        task.executeHK();

        var configEntity = taskConfigRepo.findById(SyncStockExecutionsTask.class.toString()).orElseThrow();
        var config = SyncStockExecutionsTaskConfig.fromJson(configEntity.jsonConfig());
        assertTrue(configDate.getTime() <= config.lastExecutionTimeByMarket().get(Market.HK).getTime());
    }

    @Test
    void sendTelegramFalse_willNotSendMessage() {
        task.sendTelegram = false;
        task.clientConfigJson = "not a json"; // trigger exception
        task.executeHK();
        verify(mockTelegramAPIClient, never()).sendMessage(any());
    }

    @Test
    void executeUS_usesUSMarket() {
        task.enabledByMarket = Map.of("US", true);
        task.clientConfigJson = """
        [
            {"ip":"127.0.0.1","port":1,"fundUserId":"userA","fundName":"A","accountId":1234567,"unlockCode":"","markets":["US"]}
        ]""";
        var userId = UUID.randomUUID().toString();
        task.userId = userId;
        Fund f = new Fund(userId, "A");
        fundRepo.save(f);

        var executions = new HashMap<String, Execution>();
        when(mockFutuAPIClient.getRecentExecutions(any(Date.class), eq(Market.US))).thenReturn(executions);
        
        task.executeUS();
        
        verify(mockFutuAPIClient).getRecentExecutions(any(Date.class), eq(Market.US));
    }


}
