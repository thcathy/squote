package squote.scheduletask;

import squote.domain.Market;

import java.util.List;

public record FutuClientConfig(String ip, short port, String fundUserId, String fundName, long accountId, String unlockCode, List<Market> markets) {
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
