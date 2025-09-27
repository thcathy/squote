package squote.domain;

import java.util.Currency;

public enum Market {
    US, HK;

    public static boolean isUSStockCode(String stockCode) {
        return stockCode.endsWith("." + Market.US);
    }

    public static Market getMarketByStockCode(String stockCode) {
        if (isUSStockCode(stockCode)) return US;

        return HK;
    }

    public Currency currency() {
        return switch (this) {
            case US -> Currency.getInstance("USD");
            case HK -> Currency.getInstance("HKD");
        };
    }

    public static String getBaseCodeFromTicker(String code) {
        return code.split("\\.")[0];
    }
}
