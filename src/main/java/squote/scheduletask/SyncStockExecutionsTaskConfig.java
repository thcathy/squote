package squote.scheduletask;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import squote.domain.Market;

import java.util.Date;
import java.util.Map;

public record SyncStockExecutionsTaskConfig(Map<Market, Date> lastExecutionTimeByMarket) {
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
