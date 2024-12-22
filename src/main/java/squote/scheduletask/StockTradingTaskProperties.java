package squote.scheduletask;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "stocktradingtask")
public class StockTradingTaskProperties {
    Map<String, List<String>> fundSymbols;

    public Map<String, List<String>> getFundSymbols() {
        return fundSymbols;
    }

    public void setFundSymbols(Map<String, List<String>> fundSymbols) {
        this.fundSymbols = fundSymbols;
    }
}
