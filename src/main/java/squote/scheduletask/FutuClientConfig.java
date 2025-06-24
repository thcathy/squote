package squote.scheduletask;

public record FutuClientConfig(String ip, short port, String fundUserId, String fundName, long accountId, String unlockCode) {
    public static FutuClientConfig defaultConfig() {
        return new FutuClientConfig(
                "127.0.0.1",
                (short) 80,
                "UserA",
                "FundA",
                0L,
                ""
        );
    }
}