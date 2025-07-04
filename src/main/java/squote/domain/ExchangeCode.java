package squote.domain;

import java.util.Arrays;

public class ExchangeCode {
    public enum MIC {
        XNAS(Market.US);
        
        public final Market market;
        
        MIC(Market market) {
            this.market = market;
        }
    }

    public enum Market {
        US, HK
    }
    
    public static boolean isUSStockCode(String stockCode) {
        return Arrays.stream(MIC.values())
                .filter(mic -> mic.market == Market.US)
                .anyMatch(mic -> stockCode.endsWith("." + mic.name()));
    }
}
