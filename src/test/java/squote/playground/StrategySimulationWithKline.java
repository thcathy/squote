package squote.playground;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thc.util.MathUtils;
import thc.util.TradingUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class StrategySimulationWithKline {
    private static Logger log = LoggerFactory.getLogger(StrategySimulation.class);
    private static final int BET = 3000;
    private static final int LOT_SIZE = 1;

    public static void main(String[] args) {
        new StrategySimulationWithKline().execute();
    }

    class Context {
        StrategyParam param;

        double basePrice;
        int totalBuy;
        int totalSell;
        double earning;
        double maxHoldingAmount;
        List<Execution> executions = new ArrayList<>();
        double balance;

        public Context(StrategyParam param) {
            this.param = param;
        }

        double holdingAmount() {
            return executions.stream().mapToDouble(e -> e.quantity * e.price).sum();
        }

        int holdingQuantity() { return executions.stream().mapToInt(e -> e.quantity).sum(); }

        @Override
        public String toString() {
            return String.format("basePrice=%.2f, holdings=%d (%s:%s), earning=%.0f, holdingAmt=%.0f, balance=%.0f, maxAmount=%.0f",
                    basePrice, executions.size(), totalBuy, totalSell,
                    earning, holdingAmount(), balance,
                    maxHoldingAmount);
        }
    }

    record Execution(double price, int quantity, LocalDateTime date) {}

    record Kline(LocalDateTime time, double high, double low, double open, double close, double volume) {}

    record StrategyParam(int stdDevRange, int year, double adjustment, double maxAmount) {}

    public void execute() {
        var klines = new IBKlineCsvParser().parseAllKlines();
        var params = new ArrayList<StrategyParam>();
        for (int sdRange = 9; sdRange <= 21; sdRange++) {
            for (int year = 2018; year <= 2024; year++) {
                for (double adjustment = 1.25; adjustment >= 0.25; adjustment-=0.05) {
                    params.add(new StrategyParam(sdRange, year, adjustment, BET * 20));
                }
            }
        }

        var contexts = params.stream()
                .map(param -> runSingleBatch(param, klines, BET, LOT_SIZE,
                        k -> k.time.getYear() >= param.year && k.time.getYear() <= 2024))   // multi year
//                    k -> k.time.getYear() == param.year)) // single year
                .toList();

        contexts.forEach(c ->
                log.info("Year: {}, SD range={}, adjustment={}, Context: {}", c.param.year, c.param.stdDevRange, Math.round(c.param.adjustment * 100.0) / 100.0 , c));

//        Map<Double, List<Context>> contextsByAdjustment = contexts.stream()
//                .collect(Collectors.groupingBy(c -> c.param.adjustment));
//
//        contextsByAdjustment.forEach((adjustment, contextsOfYear) -> {
//            var context = contextsOfYear.getFirst();
//            var totalEarning = contextsOfYear.stream().mapToDouble(c -> c.earning).sum();
//            log.info("SD range={}, adjustment={}, totalEarning={}", context.param.stdDevRange, adjustment, totalEarning);
//        });

//        Map<Integer, List<Context>> contextsByAdjustment = contexts.stream()
//                .collect(Collectors.groupingBy(c -> c.param.stdDevRange));
//
//        contextsByAdjustment.forEach((sdRange, contextsOfYear) -> {
//            var context = contextsOfYear.getFirst();
//            var totalEarning = contextsOfYear.stream().mapToDouble(c -> c.earning).sum();
//            log.info("SD range={}, adjustment={}, totalEarning={}", sdRange, context.param.adjustment, totalEarning);
//        });

//        Map<Integer, List<Context>> contextsByYear = contexts.stream()
//                .collect(Collectors.groupingBy(c -> c.param.year));
//
//        contextsByYear.forEach((year, contextsOfYear) -> {
//            var context = contextsOfYear.stream().max(Comparator.comparingDouble(c -> c.earning)).get();
//            log.info("Year: {}, SD range={}, adjustment={}, Context: {}", year, context.param.stdDevRange, context.param.adjustment, context);
//        });
    }

    Context runSingleBatch(StrategyParam param, List<Kline> klines, int bet, int lotSize, Predicate<Kline> klineFilter) {
        var stdDevMap = calculateStdDev(klines, param.stdDevRange);
        var context = new Context(param);

        klines = klines.stream().filter(klineFilter).toList();
        log.info("total klines={}, start on={}", klines.size(), klines.getFirst().time);
        context.basePrice = klines.getFirst().close();
        for (var kline : klines) {
            var stdDev = stdDevMap.get(kline.time.toLocalDate());
            if (stdDev == null) continue;
            if (kline.volume == 0) continue;

            var adjustedStdDev = adjustStdDev(stdDev, param.adjustment);

            var buyTriggerPrice = context.basePrice * (1 - adjustedStdDev / 100);
            if (kline.low < buyTriggerPrice && context.holdingAmount() <= param.maxAmount) {
                if (kline.high < buyTriggerPrice)
                    buyTriggerPrice = kline.high;

                var quantity = TradingUtils.roundToLotSize(bet / buyTriggerPrice, lotSize);
                log.info("{}: Buy {}@{}, low={}", kline.time, quantity, String.format("%.2f", buyTriggerPrice), String.format("%.2f", kline.low));
                context.totalBuy++;
                context.executions.addFirst(new Execution(buyTriggerPrice, quantity, kline.time));
                context.maxHoldingAmount = Math.max(context.maxHoldingAmount, context.holdingAmount());
                context.basePrice = context.executions.getFirst().price;
                log.info("{}: {}", kline.time, context);
            }

            if (canSell(context, adjustedStdDev, kline.high)) {
                var execution = context.executions.removeFirst();
                var sellTriggerPrice = execution.price * (1 + adjustedStdDev / 100);
                if (kline.low > sellTriggerPrice)
                    sellTriggerPrice = kline.low;

                var earning = (sellTriggerPrice - execution.price) * execution.quantity;
                context.earning += earning;
                context.totalSell++;
                context.basePrice = context.executions.isEmpty() ? kline.close : context.executions.getFirst().price;

                log.info("{}: Sell {}@{}, earn={}, high={}", kline.time, execution.quantity,
                        String.format("%.2f", sellTriggerPrice),
                        String.format("%.0f", earning),
                        String.format("%.2f", kline.high));
                log.info("{}: {}", kline.time, context);
            }
            context.basePrice = context.executions.isEmpty() ? kline.close : context.executions.getFirst().price;
            context.balance = context.holdingQuantity() * kline.close - context.holdingAmount();
        }
        return context;
    }

    private static double adjustStdDev(Double stdDev, double stdDevAdjustment) {
        return stdDev * stdDevAdjustment;
//        return Math.max(stdDev * stdDevAdjustment, 0.809);
//        return 1.618;
    }

    boolean canSell(Context context, double stdDev, double marketPrice) {
        if (context.executions.isEmpty()) return false;

        var sellTriggerPrice = context.executions.getFirst().price * (1 + stdDev / 100);
        return sellTriggerPrice <= marketPrice;
    }

    private Map<LocalDate, Double> calculateStdDev(List<Kline> klines, int stdDevRange) {
        // find end of day kline
        Map<LocalDate, Kline> eodKlineByDate = new HashMap<>();
        for (Kline kline : klines) {
            LocalDate date = kline.time.toLocalDate();
            eodKlineByDate.merge(date, kline, (existing, newKline) ->
                    newKline.time.isAfter(existing.time) ? newKline : existing
            );
        }
        var sortedEODKline = eodKlineByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList();

        var stdDevs = new HashMap<LocalDate, Double>();
        for (int i = stdDevRange; i < sortedEODKline.size(); i++) {
            var date = sortedEODKline.get(i).getKey();
            var closingPrices = sortedEODKline.subList(i-stdDevRange, i).stream().map(e -> e.getValue().close).toList();
            var stdDev = MathUtils.calStdDev(closingPrices);
            stdDevs.put(date, stdDev);
        }
//        stdDevs.entrySet().stream().filter(e -> e.getValue() < 0.2).forEach(e -> System.out.println(String.format("%s: %s", e.getKey(), e.getValue())));
//        System.exit(0);
        return stdDevs;
    }

    static class IBKlineCsvParser {
        private static final String klineFilesTemplate = "historical-quote/VOO-kline-5m-%s.csv";
        private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHH:mm:ss");

        List<StrategySimulationWithKline.Kline> parseAllKlines() {
            return IntStream.rangeClosed(2011, 2025)
                    .mapToObj(Integer::toString)
                    .map(i -> String.format(klineFilesTemplate, i))
                    .flatMap(f -> loadFromResourceFile(f).stream())
                    .toList();
        }

        List<Kline> loadFromResourceFile(String fileName) {
            var klines = new ArrayList<Kline>();
            try (var inputStream = this.getClass().getClassLoader().getResourceAsStream(fileName);
                 var reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    klines.add(parseKline(line));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return klines;
        }

        Kline parseKline(String str) {
            var strings = str.split(" ");
            var time = LocalDateTime.parse(strings[3] + strings[4], formatter);
            var highPrice = Double.parseDouble(strings[7].split("=")[1]);
            var lowPrice = Double.parseDouble(strings[8].split("=")[1]);
            var openPrice = Double.parseDouble(strings[6].split("=")[1]);
            var closePrice = Double.parseDouble(strings[9].split("=")[1]);
            var volume = Double.parseDouble(strings[10].split("=")[1]);
            return new Kline(time, highPrice, lowPrice, openPrice, closePrice, volume);
        }
    }

    static class FutuKlineJsonParser {
        private static final String klineFilesTemplate = "historical-quote/2800hk-kline-5m-%s.json";

        @NotNull
        private List<StrategySimulationWithKline.Kline> parseAllKlines() {
            return IntStream.rangeClosed(2017, 2025)
                    .mapToObj(Integer::toString)
                    .map(i -> String.format(klineFilesTemplate, i))
                    .flatMap(f -> loadJsonFromResourceFile(f).stream())
                    .toList();
        }

        List<Kline> loadJsonFromResourceFile(String fileName) {
            try (InputStream inputStream = StrategySimulationWithKline.class.getClassLoader().getResourceAsStream(fileName)) {
                var json = new String(inputStream.readAllBytes());
                return parseKline(json);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        List<Kline> parseKline(String json) {
            var klines = new ArrayList<Kline>();
            var objectMapper = new ObjectMapper();
            var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            try {
                var rootNode = objectMapper.readTree(json);
                var klListNode = rootNode.path("s2c").path("klList");
                for (JsonNode klineNode : klListNode) {
                    klines.add(new Kline(
                            LocalDateTime.parse(klineNode.path("time").asText(), formatter),
                            klineNode.path("highPrice").asDouble(),
                            klineNode.path("lowPrice").asDouble(),
                            klineNode.path("openPrice").asDouble(),
                            klineNode.path("closePrice").asDouble(),
                            0
                    ));
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            return klines;
        }
    }
}
