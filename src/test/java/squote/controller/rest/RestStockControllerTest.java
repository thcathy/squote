package squote.controller.rest;

import org.junit.Test;
import squote.domain.StockQuote;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RestStockControllerTest {
    RestStockController restStockController = new RestStockController();

    @Test
    public void collectAllStockQuotes_willRemoveDuplicate() {
        StockQuote[] duplicateStockQuotes = new StockQuote[]{new StockQuote("NA"), new StockQuote("NA")};
        Map<String, StockQuote> result = restStockController.collectAllStockQuotes(duplicateStockQuotes);

        assertThat(result.size()).isEqualTo(1);
    }
}