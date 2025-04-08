package squote.scheduletask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.futu.openapi.FTAPI_Conn_Qot;
import com.futu.openapi.FTAPI_Conn_Trd;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import squote.SquoteConstants.Side;
import squote.domain.HoldingStock;
import squote.domain.Order;
import squote.domain.StockQuote;
import squote.domain.repository.DailyAssetSummaryRepository;
import squote.domain.repository.FundRepository;
import squote.domain.repository.HoldingStockRepository;
import squote.service.FutuAPIClient;
import squote.service.TelegramAPIClient;

import java.util.*;
import java.util.stream.Collectors;

import static squote.SquoteConstants.Side.BUY;
import static squote.SquoteConstants.Side.SELL;

@Component
public class StockTradingTask {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Value(value = "${stocktradingtask.enabled}") public boolean enabled = false;
    @Value(value = "${stocktradingtask.stdDevRange}") int stdDevRange;
    @Value(value = "${stocktradingtask.stdDevMultiplier}") double stdDevMultiplier;
    @Value(value = "${futuOpendRsaKey}") String rsaKey;
    @Value(value = "${futuClientConfigsJson}") String clientConfigJson;
    double priceThreshold = 0.0005;

    final StockTradingTaskProperties properties;
    final DailyAssetSummaryRepository dailyAssetSummaryRepo;
    final FundRepository fundRepo;
    final HoldingStockRepository holdingStockRepository;
    final TelegramAPIClient telegramAPIClient;
    final Map<String, Double> tickSizes = Map.of("2800", 0.02, "code1", 0.02);

    public FutuAPIClientFactory futuAPIClientFactory = (ip, port) -> new FutuAPIClient(new FTAPI_Conn_Trd(), new FTAPI_Conn_Qot(), ip, port, rsaKey, true);

    @Autowired
    public StockTradingTask(DailyAssetSummaryRepository dailyAssetSummaryRepo,
                            FundRepository fundRepo,
                            HoldingStockRepository holdingStockRepository,
                            TelegramAPIClient telegramAPIClient,
                            StockTradingTaskProperties properties) {
        this.dailyAssetSummaryRepo = dailyAssetSummaryRepo;
        this.properties = properties;
        this.fundRepo = fundRepo;
        this.telegramAPIClient = telegramAPIClient;
        this.holdingStockRepository = holdingStockRepository;
    }
    
    @Scheduled(cron = "0 35-55/5 9 * * MON-FRI", zone = "Asia/Hong_Kong")
    @Scheduled(cron = "0 */5 10-15 * * MON-FRI", zone = "Asia/Hong_Kong")
    @Scheduled(cron = "0 45 16 * * MON-FRI", zone = "Asia/Hong_Kong")
    public void executeTask() {
        if (!enabled) {
            log.info("Task disabled");
            return;
        }

        try {
            innerExecute();
        } catch (Exception e) {
            log.error("Unexpected exception!" ,e);
            var message = String.format("StockTradingTask: Unexpected exception: %s \n %s", e.getMessage(), ExceptionUtils.getStackTrace(e));
            telegramAPIClient.sendMessage(message);
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

            unlockTrade(futuAPIClient, clientConfig.unlockCode());
            for (var code : tradeSymbols.getValue()) {
                processSingleSymbol(code, clientConfig, futuAPIClient);
            }
            futuAPIClient.close();
        }
    }

    public void unlockTrade(FutuAPIClient futuAPIClient, String code) {
        if (!futuAPIClient.unlockTrade(code)) {
            throw new RuntimeException("unlock trade failed");
        }
    }

    public record Execution(String code, Side side, int quantity, double price, boolean isToday, Date date) {
        @Override
        public String toString() {
            return String.format("%s %s %d@%.2f (isToday=%s) %s", side, code, quantity, price, isToday, date);
        }
    }

    private void processSingleSymbol(String code, FutuClientConfig clientConfig, FutuAPIClient futuAPIClient) {
        log.info("start process for {} in {}", code, clientConfig.fundName());
        var stdDev = getStdDev(code);
        if (stdDev.isEmpty()) {
            log.error("Cannot find std dev for {}, range={}", code, stdDevRange);
            return;
        }

        var stockQuote = futuAPIClient.getStockQuote(code);
        var holdings = holdingStockRepository.findByUserIdOrderByDate(clientConfig.fundUserId())
                .stream().filter(h -> h.getCode().equals(code) && h.getFundName().equals(clientConfig.fundName()))
                .toList();
        var allTodayExecutions = futuAPIClient.getHKStockTodayExecutions(clientConfig.accountId()).values().stream()
                .filter(e -> e.getCode().equals(code))
                .toList();
        log.info("{} holdings, {} T day executions", holdings.size(), allTodayExecutions.size());
        var buyExecutions = sortExecutions(holdings, allTodayExecutions, BUY);
        var sellExecutions = sortExecutions(holdings, allTodayExecutions, SELL);

        findBaseExecution(buyExecutions, sellExecutions)
                .ifPresent(exec -> processBaseExecution(futuAPIClient, clientConfig, stdDev.get(), exec, stockQuote));
    }

    private Optional<Double> getStdDev(String code) {
        return dailyAssetSummaryRepo.findTopBySymbolOrderByDateDesc(code)
                .flatMap(summary -> Optional.ofNullable(summary.stdDevs.get(stdDevRange)));
    }

    private void processBaseExecution(FutuAPIClient futuAPIClient, FutuClientConfig config, double stdDev, Execution execution, StockQuote stockQuote) {
        log.info("base price: {}", execution.price);    // used in test case
        log.info("process base execution: {}", execution);

        var pendingOrders = futuAPIClient.getPendingOrders(config.accountId());
        var anyPartialFilledOrder = pendingOrders.stream().filter(Order::isPartialFilled).findFirst();
        if (anyPartialFilledOrder.isPresent()) {
            var partialFillOrder = anyPartialFilledOrder.get();
            telegramAPIClient.sendMessage(String.format("%s: WARN: Partial filled %s %s@%.2f. filled=%s",
                    config.fundName(),
                    partialFillOrder.side(), partialFillOrder.quantity(), partialFillOrder.price(),
                    partialFillOrder.filledQuantity()
                    ));
            log.info("Has partial filled pending order. Skip processing");
            return;
        }

        handleOrderForBaseExecution(BUY, execution, pendingOrders, stdDev, futuAPIClient, config, stockQuote);
        handleOrderForBaseExecution(SELL, execution, pendingOrders, stdDev, futuAPIClient, config, stockQuote);
    }

    private void handleOrderForBaseExecution(Side pendingOrderSide, Execution baseExec, List<Order> pendingOrders, double stdDev, FutuAPIClient futuAPIClient, FutuClientConfig clientConfig, StockQuote stockQuote) {
        log.info("handle {} order", pendingOrders);
        if (pendingOrderSide == SELL && baseExec.side == SELL) return;

        var stockCode = baseExec.code;
        var targetPrice = calculateTargetPrice(pendingOrderSide, stockCode, baseExec, stdDev, Double.parseDouble(stockQuote.getPrice()));
        var matchedPendingOrders = pendingOrders.stream()
                .filter(o -> o.side() == pendingOrderSide && o.code().equals(stockCode))
                .toList();
        if (matchedPendingOrders.size() > 1) {
            matchedPendingOrders.forEach(o -> {
                cancelOrder(futuAPIClient, clientConfig.accountId(), o.orderId());
                telegramAPIClient.sendMessage(String.format("Cancelled order due to multiple pending (%s): %s %s %s@%.2f", clientConfig.fundName(),
                        pendingOrderSide, stockCode, o.quantity(), o.price()));
            });
        } else if (matchedPendingOrders.size() == 1) {
            var pendingOrder = matchedPendingOrders.getFirst();
            var pendingOrderPrice = matchedPendingOrders.getFirst().price();
            if (priceWithinThreshold(pendingOrderPrice, targetPrice)) {
                log.info("pendingOrderPrice {} within threshold. do nothing", pendingOrderPrice);
                return; // do nothing
            } else if (baseExec.side == SELL && pendingOrderPrice > targetPrice) {
                log.info("Base is sell. pendingOrderPrice {} > target price {}. do nothing", pendingOrderPrice, targetPrice);
                return; // do nothing
            }

            var pendingOrderId = pendingOrder.orderId();
            log.info("pending order price {} over threshold {}. Going to cancel order id={}", pendingOrderPrice, targetPrice, pendingOrderId);
            cancelOrder(futuAPIClient, clientConfig.accountId(), pendingOrderId);
            telegramAPIClient.sendMessage(String.format("Cancelled order (%s): %s %s %s@%.2f", clientConfig.fundName(),
                    pendingOrder.side(), stockCode,pendingOrder.quantity(), pendingOrderPrice));
        }

        placeOrder(futuAPIClient, clientConfig, baseExec, stockCode, pendingOrderSide, targetPrice);
    }

    private double calculateTargetPrice(Side orderSide, String code, Execution baseExec, double stdDev, double marketPrice) {
        var basePrice = baseExec.price;
        var modifiedStdDevPercentage = (stdDev * stdDevMultiplier / 100);
        var targetPrice = orderSide == SELL ? basePrice * (1 + modifiedStdDevPercentage) : basePrice / (1 + modifiedStdDevPercentage);

        // handle target price far from market price
        if (orderSide == BUY) {
            var priceAdjustmentFactor = 1 + (modifiedStdDevPercentage / 2);
            if (baseExec.side == BUY) {
                while (targetPrice > marketPrice) {
                    targetPrice = targetPrice / priceAdjustmentFactor;
                }
            } else {
                var minBuyPrice = marketPrice / (1 + stdDev / 100);   // choose stdDev explicitly
                while (targetPrice < minBuyPrice) {
                    targetPrice = targetPrice * priceAdjustmentFactor;
                }
            }
        }

        var tickSize = tickSizes.getOrDefault(code, 0.01);
        targetPrice = orderSide == BUY ? Math.floor(targetPrice / tickSize) * tickSize : Math.ceil(targetPrice / tickSize) * tickSize;
        targetPrice = (double) Math.round(targetPrice * 1000) / 1000;
        log.info("{}: targetPrice={}, basePrice={}, stdDev={}, mktPx={}", orderSide, targetPrice, basePrice, stdDev, marketPrice);
        return targetPrice;
    }

    private void placeOrder(FutuAPIClient futuAPIClient, FutuClientConfig config, Execution execution, String stockCode, Side side, double price) {
        var placeOrderResponse = futuAPIClient.placeOrder(config.accountId(), side, stockCode, execution.quantity, price);

        if (placeOrderResponse.errorCode() > 0) {
            log.error("Cannot place order, error cod={}, message={}", placeOrderResponse.errorCode(), placeOrderResponse.message());
            throw new RuntimeException("Cannot place order");
        }

        String placedMessage = String.format("Placed order (%s): %s %s %d@%.2f",
                config.fundName(),
                side, stockCode, execution.quantity, price);
        log.info(placedMessage);
        telegramAPIClient.sendMessage(placedMessage);
    }

    private void cancelOrder(FutuAPIClient futuAPIClient, long accountId, long pendingOrderId) {
        var cancelOrderResponse = futuAPIClient.cancelOrder(accountId, pendingOrderId);

        if (cancelOrderResponse.errorCode() > 0) {
            String errorMessage = String.format("Cannot cancel order %s, error code=%s, message=%s",
                    pendingOrderId,
                    cancelOrderResponse.errorCode(),
                    cancelOrderResponse.message());
            throw new RuntimeException(errorMessage);
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
        if (buyExecutions.isEmpty() && sellExecutions.size() == 1) return Optional.ofNullable(sellExecutions.getFirst());

        int buyIndex = 0, sellIndex = 0;
        var cloneBuyExecutions = new ArrayList<>(buyExecutions);
        var cloneSellExecutions = new ArrayList<>(sellExecutions);

        while (buyIndex < buyExecutions.size()) {
            if (sellExecutions.get(sellIndex).price > buyExecutions.get(buyIndex).price) {
                if (sellExecutions.get(sellIndex).date.after(buyExecutions.get(buyIndex).date)) {
                    cloneBuyExecutions.remove(buyIndex);
                    cloneSellExecutions.remove(sellIndex);
                    if (cloneBuyExecutions.isEmpty() && cloneSellExecutions.isEmpty()) return Optional.ofNullable(sellExecutions.getFirst());
                    return findBaseExecution(cloneBuyExecutions, cloneSellExecutions);
                } else {
                    buyIndex++;
                }
            } else {
                log.error("Unexpected executions: {} < {}", sellExecutions.get(sellIndex), buyExecutions.get(buyIndex));
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    private void printExecutions(String title, List<Execution> executions) {
        log.info(title);
        executions.forEach(execution -> log.info("{}@{} at {}", execution.quantity, execution.price, execution.date));
    }

    private List<Execution> sortExecutions(List<HoldingStock> holdings,
                                           List<squote.domain.Execution> allTodayExecutions,
                                           Side side) {
        List<Execution> executions = getExecutionsBySide(holdings, side);
        List<Execution> todayExecutions = allTodayExecutions.stream()
                .filter(e -> e.getSide() == side)
                .map(this::toExecution)
                .toList();
        executions.addAll(todayExecutions);
        executions.sort(
                Comparator.comparingDouble(Execution::price)
                        .thenComparing(Execution::date, Comparator.reverseOrder())
        );
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
                true, new Date(e.getTime()));
    }

    private Execution toExecution(HoldingStock holding) {
        return new Execution(holding.getCode(), holding.getSide(),
                holding.getQuantity(),
                holding.getGross().doubleValue() / (holding.getQuantity()),
                false, holding.getDate());
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
