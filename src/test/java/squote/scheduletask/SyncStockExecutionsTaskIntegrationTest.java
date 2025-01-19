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
import squote.domain.TaskConfig;
import squote.domain.repository.FundRepository;
import squote.domain.repository.HoldingStockRepository;
import squote.domain.repository.TaskConfigRepository;
import squote.service.EmailService;
import squote.service.FutuAPIClient;
import squote.service.TelegramAPIClient;
import squote.service.UpdateFundByHoldingService;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyShort;
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
        when(mockFactory.build(any(), anyShort())).thenReturn(mockFutuAPIClient);
        when(mockTelegramAPIClient.sendMessage(any(String.class))).thenReturn(List.of("Message sent"));

        task = new SyncStockExecutionsTask();
        task.holdingRepo = holdingRepo;
        task.fundRepo = fundRepo;
        task.emailService = mockEmailService;
        task.telegramAPIClient = mockTelegramAPIClient;
        task.sendTelegram = true;
        task.enabled = true;
        task.futuAPIClientFactory = mockFactory;
        task.updateFundService = updateFundByHoldingService;
        task.taskConfigRepo = taskConfigRepo;
        task.clientConfigJson = """
        [
            {"ip":"127.0.0.1","port":1,"fundName":"A","accountId": 1234567}
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
        executions.put(exec.getOrderId(), exec);

        when(mockFutuAPIClient.getHKStockExecutions(eq(1234567L), any())).thenReturn(executions);
        task.executeTask();

        var holdings = Lists.newArrayList(holdingRepo.findAll());
        assertEquals(1, holdings.size());
        var holding = holdings.getFirst();
        assertEquals("2800", holding.getCode());
        assertEquals(userId, holding.getUserId());
        assertEquals("A", holding.getFundName());
        assertEquals(1000, holding.getQuantity());
        assertEquals(execDate, holding.getDate());
        verify(mockTelegramAPIClient, times(1)).sendMessage(startsWith("Created holding for A"));

        var fund = fundRepo.findByUserIdAndName(userId, "A").get();
        assertEquals(-19.43, fund.getProfit().doubleValue());   // due to fee
        var fundHolding = fund.getHoldings().get("2800");
        assertEquals(2000, fundHolding.getQuantity().intValue());
        assertEquals(50000, fundHolding.getGross().intValue());

        var configEntity = taskConfigRepo.findById(SyncStockExecutionsTask.class.toString()).orElseThrow();
        var config = SyncStockExecutionsTask.SyncStockExecutionsTaskConfig.fromJson(configEntity.jsonConfig());
        assertEquals(execDate, config.lastExecutionTime());

        // run the task again will not repeat same fill id
        task.executeTask();
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
        var jsonConfig = SyncStockExecutionsTask.SyncStockExecutionsTaskConfig.toJson(
                new SyncStockExecutionsTask.SyncStockExecutionsTaskConfig(configDate));
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
        executions.put(exec.getOrderId(), exec);
        when(mockFutuAPIClient.getHKStockExecutions(eq(1234567L), any())).thenReturn(executions);

        task.executeTask();

        var configEntity = taskConfigRepo.findById(SyncStockExecutionsTask.class.toString()).orElseThrow();
        var config = SyncStockExecutionsTask.SyncStockExecutionsTaskConfig.fromJson(configEntity.jsonConfig());
        assertTrue(configDate.getTime() < config.lastExecutionTime().getTime());
    }

    @Test
    void sendTelegramFalse_willNotSendMessage() {
        task.sendTelegram = false;
        task.clientConfigJson = "not a json"; // trigger exception
        task.executeTask();
        verify(mockTelegramAPIClient, never()).sendMessage(any());
    }
}
