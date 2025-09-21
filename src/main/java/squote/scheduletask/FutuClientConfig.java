package squote.scheduletask;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import squote.domain.Market;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record FutuClientConfig(String ip, short port, String fundUserId, String fundName, long accountId, String unlockCode, List<Market> markets) {
    private static final Logger log = LoggerFactory.getLogger(FutuClientConfig.class);

    public static Map<String, FutuClientConfig> parseFutuClientConfigs(String json) {
        try
        {
            var mapper = new ObjectMapper();
            var clientConfigs = mapper.readValue(json, FutuClientConfig[].class);
            return Arrays.stream(clientConfigs).collect(Collectors.toMap(FutuClientConfig::fundName, o -> o));
        } catch (Exception e) {
            log.error("Cannot parse config: {}", json ,e);
        }
        return new HashMap<>();
    }

    public static FutuClientConfig defaultConfig() {
        return new FutuClientConfig(
                "127.0.0.1",
                (short) 80,
                "UserA",
                "FundA",
                0L,
                "",
                List.of(Market.HK)
        );
    }
}
