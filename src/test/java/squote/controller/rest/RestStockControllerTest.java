package squote.controller.rest;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import squote.IntegrationTest;
import squote.SquoteConstants;
import squote.domain.HoldingStock;
import squote.domain.StockQuote;
import squote.domain.repository.HoldingStockRepository;
import squote.security.AuthenticationServiceStub;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

public class RestStockControllerTest extends IntegrationTest {
    @Autowired RestStockController restStockController;
    @Autowired HoldingStockRepository holdingStockRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired AuthenticationServiceStub authenticationServiceStub;

    @AfterEach
    public void resetStub() {
        authenticationServiceStub.userId = authenticationServiceStub.TESTER_USERID;
    }

    @Test
    public void collectAllStockQuotes_willRemoveDuplicate() {
        StockQuote[] duplicateStockQuotes = new StockQuote[]{new StockQuote("NA"), new StockQuote("NA")};
        Map<String, StockQuote> result = restStockController.collectAllStockQuotes(duplicateStockQuotes);

        assertThat(result.size()).isEqualTo(1);
    }

    @Test
    public void testStockQuery_saveAndLoad() {
        String result = restStockController.loadQuery();
        assertThat(result).isEqualTo("");

        String codes = "123,321,123456";
        result = restStockController.saveQuery(codes);
        assertThat(result).isEqualTo(codes);

        result = restStockController.loadQuery();
        assertThat(result).isEqualTo(codes);

        authenticationServiceStub.userId = "another user id";
        result = restStockController.loadQuery();
        assertThat(result).isEqualTo("");
    }

    @Test
    public void test_listHolding() {
        Iterable<HoldingStock> holdings = restStockController.listHolding();
        long countBeforeStart = StreamSupport.stream(holdings.spliterator(), false).count();

        holdingStockRepository.save(createSell2800Holding(authenticationServiceStub.TESTER_USERID));

        holdings = restStockController.listHolding();
        long count = StreamSupport.stream(holdings.spliterator(), false).count();
        assertThat(count).isEqualTo(countBeforeStart + 1);

        authenticationServiceStub.userId = "another user id";
        holdings = restStockController.listHolding();
        count = StreamSupport.stream(holdings.spliterator(), false).count();
        assertThat(count).isEqualTo(0);
    }

    @Test
    public void test_deleteHolding() {
        authenticationServiceStub.userId = UUID.randomUUID().toString();
        HoldingStock holding = holdingStockRepository.save(createSell2800Holding(authenticationServiceStub.userId));

        Iterable<HoldingStock> holdings = restStockController.listHolding();
        long count = StreamSupport.stream(holdings.spliterator(), false).count();
        assertThat(count).isEqualTo(1);

        holdings = restStockController.delete(holding.getId());
        count = StreamSupport.stream(holdings.spliterator(), false).count();
        assertThat(count).isEqualTo(0);
    }

    @Test
    public void test_quote_supportMultiUser() throws ExecutionException, InterruptedException {
        authenticationServiceStub.userId = UUID.randomUUID().toString();
        holdingStockRepository.save(createSell2800Holding(authenticationServiceStub.userId));

        Map<String, Object> resultMap = restStockController.quote("");
        assertThat(((List)resultMap.get("holdings")).size()).isEqualTo(1);
        assertThat(((List)resultMap.get("funds")).size()).isEqualTo(0);

        authenticationServiceStub.userId = authenticationServiceStub.TESTER_USERID;
        resultMap = restStockController.quote("");
        assertThat(((List)resultMap.get("holdings")).size()).isEqualTo(0);
        assertThat(((List)resultMap.get("funds")).size()).isEqualTo(0);

    }

    private HoldingStock createSell2800Holding(String userId) {
        return new HoldingStock(userId, "2800", SquoteConstants.Side.SELL, 300, new BigDecimal("8190"), new Date(), null);
    }
}
