package squote.scheduletask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.futu.openapi.FTAPI_Conn_Trd;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import squote.SquoteConstants.Side;
import squote.domain.HoldingStock;
import squote.domain.Order;
import squote.domain.repository.DailyAssetSummaryRepository;
import squote.domain.repository.FundRepository;
import squote.domain.repository.HoldingStockRepository;
import squote.service.FutuAPIClient;

import java.util.*;
import java.util.stream.Collectors;

import static squote.SquoteConstants.Side.BUY;
import static squote.SquoteConstants.Side.SELL;

@Component
public class StockTradingTask {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Value(value = "${stocktradingtask.enabled}") boolean enabled;
    @Value(value = "${stocktradingtask.stdDevRange}") int stdDevRange;
    @Value(value = "${stocktradingtask.stdDevMultiplier}") double stdDevMultiplier;
    @Value(value = "${futuOpendRsaKey}") String rsaKey;
    @Value(value = "${futuClientConfigsJson}") String clientConfigJson;
    double priceThreshold = 0.002;

    final StockTradingTaskProperties properties;
    final DailyAssetSummaryRepository dailyAssetSummaryRepo;
    final FundRepository fundRepo;
    final HoldingStockRepository holdingStockRepository;
    final Map<String, Double> tickSizes = Map.of("2800", 0.02, "code1", 0.02);

    public FutuAPIClientFactory futuAPIClientFactory = (ip, port) -> new FutuAPIClient(new FTAPI_Conn_Trd(), ip, port, rsaKey, true);

    public StockTradingTask(DailyAssetSummaryRepository dailyAssetSummaryRepo,
                            FundRepository fundRepo,
                            HoldingStockRepository holdingStockRepository,
                            StockTradingTaskProperties properties) {
        this.dailyAssetSummaryRepo = dailyAssetSummaryRepo;
        this.properties = properties;
        this.fundRepo = fundRepo;
        this.holdingStockRepository = holdingStockRepository;
    }

    @PostConstruct
    public void init() {executeTask();}

//    @Scheduled(cron = "0 30 16 * * MON-FRI", zone = "Asia/Hong_Kong")
    public void executeTask() {
        if (!enabled) {
            log.info("Task disabled");
            return;
        }

        try {
            innerExecute();
        } catch (Exception e) {
            log.error("Unexpected exception!" ,e);
        }
    }

    public void innerExecute() {
        log.info("Starting stock trading task");
        var futuClientConfigs = parseFutuClientConfigs();

        for (var tradeSymbols : properties.fundSymbols.entrySet()) {
            var fundName = tradeSymbols.getKey();
            var clientConfig = futuClientConfigs.get(fundName);
            if (clientConfig == null) {
                log.warn("cannot find client config for fund: {}", fundName);
                continue;
            }
            FutuAPIClient futuAPIClient = futuAPIClientFactory.build(clientConfig.ip(), clientConfig.port());

            for (var code : tradeSymbols.getValue()) {
                processSingleSymbol(code, clientConfig, futuAPIClient);
            }
        }
    }

    public record Execution(String code, Side side, int quantity, double price, boolean isToday) {
        @Override
        public String toString() {
            return String.format("%s %s %d@%.2f (isToday=%s)", side, code, quantity, price, isToday);
        }
    }

    private void processSingleSymbol(String code, FutuClientConfig clientConfig, FutuAPIClient futuAPIClient) {
        var stdDev = dailyAssetSummaryRepo.findTopBySymbolOrderByDateDesc(code)
                .flatMap(summary -> Optional.ofNullable(summary.stdDevs.get(stdDevRange)));
        if (stdDev.isEmpty()) {
            log.error("Cannot find std dev for {}, range={}", code, stdDevRange);
            return;
        }

        var holdings = holdingStockRepository.findByUserIdOrderByDate(clientConfig.fundUserId())
                .stream().filter(h -> h.getCode().equals(code)).toList();
        var allTodayExecutions = futuAPIClient.getHKStockTodayExecutions(clientConfig.accountId()).values().stream()
                .filter(e -> e.getCode().equals(code))
                .toList();

        var buyExecutions = createSortedExecutions(holdings, allTodayExecutions, BUY);
        var sellExecutions = createSortedExecutions(holdings, allTodayExecutions, SELL);

        findBaseExecution(buyExecutions, sellExecutions)
                .ifPresent(exec -> processBaseExecution(futuAPIClient, clientConfig.accountId(), stdDev.get(), exec));
    }

    private void processBaseExecution(FutuAPIClient futuAPIClient, long accountId, double stdDev, Execution execution) {
        log.info("base price: {}", execution.price);    // used in test case
        log.info("base execution: {}", execution);

        var pendingOrders = futuAPIClient.getPendingOrders(accountId);
        handleOrderForBaseExecution(BUY, execution, pendingOrders, stdDev, futuAPIClient, accountId);
        handleOrderForBaseExecution(SELL, execution, pendingOrders, stdDev, futuAPIClient, accountId);
    }

    private void handleOrderForBaseExecution(Side pendingOrderSide, Execution baseExec, List<Order> pendingOrders, double stdDev, FutuAPIClient futuAPIClient, long accountId) {
        if (pendingOrderSide == SELL && baseExec.side == SELL) return;

        var stockCode = baseExec.code;
        var targetPrice = calculateTargetPrice(pendingOrderSide, stockCode, baseExec.price, stdDev);
        var pendingOrder = pendingOrders.stream()
                .filter(o -> o.side() == pendingOrderSide && o.code().equals(stockCode) && o.quantity() == baseExec.quantity)
                .findFirst();
        if (pendingOrder.isPresent()) {
            var pendingOrderPrice = pendingOrder.get().price();
            if (priceWithinThreshold(pendingOrderPrice, targetPrice)) {
                return; // do nothing
            }

            var pendingOrderId = pendingOrder.get().orderId();
            log.info("pending order price {} over threshold {}. Going to cancel order id={}", pendingOrderPrice, targetPrice, pendingOrderId);
            cancelOrder(futuAPIClient, accountId, pendingOrderId);
        }

        placeOrder(futuAPIClient, accountId, baseExec, stockCode, pendingOrderSide, targetPrice);
    }

    private double calculateTargetPrice(Side side, String code, double basePrice, double stdDev) {
        var targetPrice = side == SELL ? basePrice * (1 + (stdDev * stdDevMultiplier / 100)) : basePrice / (1 + (stdDev * stdDevMultiplier / 100));
        var tickSize = tickSizes.getOrDefault(code, 0.01);
        targetPrice = Math.round(targetPrice / tickSize) * tickSize;
        targetPrice = side == BUY ? targetPrice - tickSize : targetPrice + tickSize;   // 1 tick size advanced for fee
        return Math.round(targetPrice/ tickSize) * tickSize;
    }

    private void placeOrder(FutuAPIClient futuAPIClient, long accountId, Execution execution, String stockCode, Side side, double price) {
        log.info("place order: {} {} {}@{}", side, stockCode, execution.quantity, price);
        var placeOrderResponse = futuAPIClient.placeOrder(accountId, side, stockCode, execution.quantity, price);

        if (placeOrderResponse.errorCode() > 0) {
            log.error("Cannot place order, error cod={}, message={}", placeOrderResponse.errorCode(), placeOrderResponse.message());
            throw new RuntimeException("Cannot place order");
        }
    }

    private void cancelOrder(FutuAPIClient futuAPIClient, long accountId, long pendingOrderId) {
        var cancelOrderResponse = futuAPIClient.cancelOrder(accountId, pendingOrderId);

        if (cancelOrderResponse.errorCode() > 0) {
            log.error("Cannot cancel order {}, error cod={}, message={}", pendingOrderId, cancelOrderResponse.errorCode(), cancelOrderResponse.message());
            throw new RuntimeException("Cannot cancel order " + pendingOrderId);
        }
    }

    public boolean priceWithinThreshold(double p1, double p2) {
        double difference = Math.abs(p1 - p2);
        double tolerance = Math.max(Math.abs(p1), Math.abs(p2)) * priceThreshold;
        return difference <= tolerance;
    }

    private Optional<Execution> findBaseExecution(List<Execution> buyExecutions, List<Execution> sellExecutions) {
        if (buyExecutions.isEmpty() && sellExecutions.isEmpty()) return Optional.empty();

        if (sellExecutions.isEmpty()) return buyExecutions.stream().findFirst();

        int buyIndex = 0, sellIndex = 0;

        while (buyIndex < buyExecutions.size() && sellIndex < sellExecutions.size()) {
            if (sellExecutions.get(sellIndex).price > buyExecutions.get(buyIndex).price) {
                buyIndex++;
                sellIndex++;
            } else {
                log.error("Unexpected executions: {} < {}", sellExecutions.get(sellIndex), buyExecutions.get(buyIndex));
                return Optional.empty();
            }
        }

        if (buyIndex < buyExecutions.size()) return Optional.of(buyExecutions.get(buyIndex));

        if (sellExecutions.size() == 1) return sellExecutions.stream().findFirst();

        return Optional.empty();
    }

    private void printExecutions(String title, List<Execution> executions) {
        log.info(title);
        executions.forEach(execution -> log.info("{}@{}", execution.quantity, execution.price));
    }

    private List<Execution> createSortedExecutions(List<HoldingStock> holdings,
                                                   List<squote.domain.Execution> allTodayExecutions,
                                                   Side side) {
        List<Execution> executions = getExecutionsBySide(holdings, side);
        List<Execution> todayExecutions = allTodayExecutions.stream()
                .filter(e -> e.getSide() == side)
                .map(this::toExecution)
                .toList();
        executions.addAll(todayExecutions);
        executions.sort(Comparator.comparingDouble(Execution::price));
        printExecutions(side + " executions:", executions);
        return executions;
    }

    private List<Execution> getExecutionsBySide(List<HoldingStock> holdings, Side side) {
        return holdings.stream()
                .filter(h -> h.getSide() == side)
                .map(this::toExecution)
                .collect(Collectors.toList());
    }

    private Execution toExecution(squote.domain.Execution e) {
        return new Execution(e.getCode(), e.getSide(),
                e.getQuantity().intValue(),
                e.getPrice().doubleValue(),
                true);
    }

    private Execution toExecution(HoldingStock holding) {
        return new Execution(holding.getCode(), holding.getSide(),
                holding.getQuantity(),
                holding.getGross().doubleValue() / (holding.getQuantity()),
                false
        );
    }

    private Map<String, FutuClientConfig> parseFutuClientConfigs() {
        try
        {
            var mapper = new ObjectMapper();
            var clientConfigs = mapper.readValue(clientConfigJson, FutuClientConfig[].class);
            return Arrays.stream(clientConfigs).collect(Collectors.toMap(FutuClientConfig::fundName, o -> o));
        } catch (Exception e) {
            log.error("Cannot parse config: {}", clientConfigJson ,e);
        }
        return new HashMap<>();
    }
}