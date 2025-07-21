package squote.domain;

public enum Market {
    US, HK;

    public static boolean isUSStockCode(String stockCode) {
        return stockCode.endsWith("." + Market.US);
    }

    public static Market getMarketByStockCode(String stockCode) {
        if (isUSStockCode(stockCode)) return US;

        return HK;
    }

    public static String getBaseCodeFromTicker(String code) {
        return code.split("\\.")[0];
    }
}
