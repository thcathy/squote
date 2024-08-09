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
import squote.domain.repository.FundRepository;
import squote.domain.repository.HoldingStockRepository;
import squote.service.EmailService;
import squote.service.FutuAPIClient;
import squote.service.UpdateFundByHoldingService;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.Mockito.*;
import static squote.SquoteConstants.Side.BUY;

@AutoConfigureMockMvc
class SyncStockExecutionsTaskIntegrationTest extends IntegrationTest {
    @Autowired SyncStockExecutionsTask task;
    @Autowired FundRepository fundRepo;
    @Autowired HoldingStockRepository holdingRepo;
    @Autowired UpdateFundByHoldingService updateFundByHoldingService;
    // @Autowired EmailService emailService;    // for actual SMTP testing

    FutuAPIClientFactory mockFactory = Mockito.mock(FutuAPIClientFactory.class);
    FutuAPIClient mockFutuAPIClient = Mockito.mock(FutuAPIClient.class);
    EmailService emailService = Mockito.mock(EmailService.class);


    @BeforeEach
    void init() {
        holdingRepo.deleteAll();
        fundRepo.deleteAll();
        when(mockFactory.build(any(), anyShort())).thenReturn(mockFutuAPIClient);

        task = new SyncStockExecutionsTask();
        task.holdingRepo = holdingRepo;
        task.fundRepo = fundRepo;
        task.emailService = emailService;
        task.enabled = true;
        task.futuAPIClientFactory = mockFactory;
        task.updateFundService = updateFundByHoldingService;
        task.clientConfigJson = """
        [
            {"ip":"127.0.0.1","port":1,"fundName":"A","accountId": 1234567}
        ]""";
    }

    @Test
    void executeTask_canBeDisabled() {
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
        verify(emailService, times(1)).sendEmail(any(), any(), any());

        var holdings = Lists.newArrayList(holdingRepo.findAll());
        assertEquals(1, holdings.size());
        var holding = holdings.getFirst();
        assertEquals("2800", holding.getCode());
        assertEquals(userId, holding.getUserId());
        assertEquals("A", holding.getFundName());
        assertEquals(1000, holding.getQuantity());
        assertEquals(execDate, holding.getDate());

        var fund = fundRepo.findByUserIdAndName(userId, "A").get();
        assertEquals(-19.43, fund.getProfit().doubleValue());   // due to fee
        var fundHolding = fund.getHoldings().get("2800");
        assertEquals(2000, fundHolding.getQuantity().intValue());
        assertEquals(50000, fundHolding.getGross().intValue());

        // run the task again will not repeat same fill id
        task.executeTask();
        verify(emailService, times(2)).sendEmail(any(), any(), any());
        fund = fundRepo.findByUserIdAndName(userId, "A").get();
        assertEquals(-19.43, fund.getProfit().doubleValue());   // due to fee
        fundHolding = fund.getHoldings().get("2800");
        assertEquals(2000, fundHolding.getQuantity().intValue());
        assertEquals(50000, fundHolding.getGross().intValue());
    }
}
