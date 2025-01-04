package squote.playground;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import squote.domain.DailyStockQuote;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class StrategySimulation {
    private static Logger log = LoggerFactory.getLogger(StrategySimulation.class);

    private static final int STD_DEV_RANGE = 20;
    private static final double STD_DEV_MULTIPLIER = 0.95;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static final int BET = 130000;
    private static final int LOT_SIZE = 500;
    private static final String file = "historical-quote/historical-quote-2800.json";

    public static void main(String[] args) throws IOException {
        new StrategySimulation().execute();
    }

    public class Context {
        double basePrice;
        int totalBuy;
        int totalSell;
        double earning;
        int noExecutionDay;
        double latestStdDev = 100;
        double maxHoldingAmount;
        int totalDayTrade;
        double dayTradeEarning;
        List<Execution> holdingPrices = new ArrayList<>();

        double holdingAmount() {
            return holdingPrices.stream().mapToDouble(e -> e.quantity * e.price).sum();
        }

        @Override
        public String toString() {
            return String.format("basePrice=%.2f, holdings=%d (%s:%s), earning=%.0f, holdingAmt=%.0f, maxAmount=%.0f, dayTrade=%d, dayTradeEarning=%.2f",
                    basePrice, holdingPrices.size(), totalBuy, totalSell,
                    earning, holdingAmount(), maxHoldingAmount,
                    totalDayTrade, dayTradeEarning);
        }
    }

    record Execution(double price, int quantity, Date date) {}

    public void execute() throws IOException {
        var quotes = loadJsonFromResourcesFile(file);
        // quotes = quotes.subList(40, quotes.size());
        log.info("total quotes={}, start on={}", quotes.size(), quotes.get(0).date());
        var context = new Context();
        context.basePrice = quotes.get(STD_DEV_RANGE).close();

        for (int i = STD_DEV_RANGE; i < quotes.size(); i++)
        {
            var executed = false;
            var quote = quotes.get(i);
            if (quote.open() == 0 || quote.high() == 0 || quote.low() == 0) continue;

            var dateString = dateFormat.format(quote.date());
            var adjustedStdDev = context.latestStdDev * STD_DEV_MULTIPLIER;

            for (int j = context.holdingPrices.size() - 1; j >= 0; j--) {
                var sellTriggerPrice = context.holdingPrices.get(j).price * (1 + adjustedStdDev / 100);
                if (quote.high() > sellTriggerPrice) {  // sell holding
                    var execution = context.holdingPrices.remove(j);
                    executed = true;
                    var earning = execution.price * execution.quantity * (adjustedStdDev / 100);
                    context.earning += earning;
                    context.totalSell++;
                    context.basePrice = sellTriggerPrice;
                    if (execution.date.equals(quote.date())) {
                        context.totalDayTrade++;
                        context.dayTradeEarning += earning;
                    }

                    log.info("{}: Sell @{}, earn={}, high={}", dateString,
                            String.format("%.2f", sellTriggerPrice),
                            String.format("%.0f", earning),
                            String.format("%.2f", quote.high()));
                    log.info("{}: {}", dateString, context);
                }
            }


            var buyTriggerPrice = context.basePrice * (1 - adjustedStdDev / 100);
            if (quote.low() < buyTriggerPrice) {    // buy more
                log.info("{}: Buy @{}, low={}", dateString, String.format("%.2f", buyTriggerPrice), String.format("%.2f", quote.low()));
                context.basePrice = buyTriggerPrice;
                context.totalBuy++;
                context.holdingPrices.add(new Execution(buyTriggerPrice, roundToLotSize(BET / buyTriggerPrice, LOT_SIZE), quote.date()));
                executed = true;
                log.info("{}: {}", dateString, context);
            }

            if (context.holdingPrices.isEmpty()) {
                context.basePrice = quote.close();
            }
            if (!executed) context.noExecutionDay++;
//            log.info("{}: basePrice={}, holdings={} ({}:{}), earning={}", dateString, String.format("%.2f", context.basePrice), context.holdingPrices.size(), context.totalBuy, context.totalSell, String.format("%.0f", context.earning));
            context.latestStdDev = calStdDev(quotes.subList(i- STD_DEV_RANGE, i).stream().map(DailyStockQuote::close).toList());
            context.maxHoldingAmount = Math.max(context.maxHoldingAmount, context.holdingAmount());
        }
        log.info("No Execution day={}", context.noExecutionDay);
        log.info("Final context={}", context);
    }

    public static int roundToLotSize(double amount, int lotSize) {
        double qty = amount / lotSize;
        int roundedQty = (int) Math.round(qty);
        return roundedQty * lotSize;
    }

    public static double calStdDev(List<Double> data) {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (int i = 1; i < data.size(); i++) {
            if (data.get(i) == 0 || data.get(i-1) == 0) continue;
            var change = calculatePercentageChange(data.get(i-1), data.get(i));
            stats.addValue(change);
        }
        return stats.getStandardDeviation();
    }

    public static double calculatePercentageChange(double originalValue, double newValue) {
        if (originalValue == 0) {
            throw new IllegalArgumentException("Original value cannot be zero.");
        }
        return ((newValue - originalValue) / originalValue) * 100;
    }

    private static List<DailyStockQuote> loadJsonFromResourcesFile(String filename) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        try (InputStream inputStream = StrategySimulation.class.getClassLoader().getResourceAsStream(filename)) {
            return objectMapper.readValue(inputStream,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, DailyStockQuote.class));
        }
    }
}
