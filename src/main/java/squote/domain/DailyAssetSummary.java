package squote.domain;

import org.springframework.data.annotation.Id;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DailyAssetSummary {
    @Id
    private String id;
    public String symbol;
    public Date date;
    public Map<Integer, Double> stdDevs = new HashMap<>();

    public DailyAssetSummary() {}

    public DailyAssetSummary(String symbol, Date date) {
        this.symbol = symbol;
        this.date = date;
        this.id = symbol + "-" + date.getTime();
    }
}
