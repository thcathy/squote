package squote.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import squote.SquoteConstants.Side;
import squote.domain.*;
import squote.domain.repository.DailyAssetSummaryRepository;
import squote.domain.repository.FundRepository;
import squote.domain.repository.HoldingStockRepository;
import squote.scheduletask.FutuClientConfig;

import java.util.*;
import java.util.stream.Collectors;

import static squote.SquoteConstants.Side.BUY;
import static squote.SquoteConstants.Side.SELL;

@Service
public class StockTradingAlgoService {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    double priceThresholdPercent = 0.0005;

    final DailyAssetSummaryRepository dailyAssetSummaryRepo;
    final FundRepository fundRepo;
    final HoldingStockRepository holdingStockRepository;
    final WebParserRestService webParserRestService;
    final TelegramAPIClient telegramAPIClient;
    final Map<String, Double> tickSizes = Map.of("2800", 0.02, "code1", 0.02);

    @Autowired
    public StockTradingAlgoService(DailyAssetSummaryRepository dailyAssetSummaryRepo,
                                   FundRepository fundRepo,
                                   HoldingStockRepository holdingStockRepository,
                                   WebParserRestService webParserService,
                                   TelegramAPIClient telegramAPIClient) {
        this.dailyAssetSummaryRepo = dailyAssetSummaryRepo;
        this.fundRepo = fundRepo;
        this.telegramAPIClient = telegramAPIClient;
        this.holdingStockRepository = holdingStockRepository;
        this.webParserRestService = webParserService;
    }

    public record Execution(String code, Side side, int quantity, double price, boolean isToday, Date date) {
        @Override
        public String toString() {
            return String.format("%s %s %d@%.2f (isToday=%s) %s", side, code, quantity, price, isToday, date);
        }
    }

    public void processSingleSymbol(Fund fund, ExchangeCode.Market market, AlgoConfig algoConfig, FutuClientConfig clientConfig, IBrokerAPIClient brokerAPIClient) {
        log.info("start process for {} in {}", algoConfig.code(), fund.name);
        var stdDev = getStdDev(algoConfig.code(), algoConfig.stdDevRange());
        if (stdDev.isEmpty()) {
            log.error("Cannot find std dev for {}, range={}", algoConfig.code(), algoConfig.stdDevRange());
            return;
        }

        var stockQuote = getStockQuote(algoConfig.code(), brokerAPIClient);
        var holdings = holdingStockRepository.findByUserIdOrderByDate(fund.userId)
                .stream().filter(h -> h.getCode().equals(algoConfig.code()) && h.getFundName().equals(fund.name))
                .toList();
        var allTodayExecutions = brokerAPIClient.getStockTodayExecutions(market).values().stream()
                .filter(e -> e.getCode().equals(algoConfig.code()))
                .toList();
        log.info("{} holdings, {} T day executions", holdings.size(), allTodayExecutions.size());
        var buyExecutions = sortExecutions(holdings, allTodayExecutions, BUY);
        var sellExecutions = sortExecutions(holdings, allTodayExecutions, SELL);

        findBaseExecution(buyExecutions, sellExecutions)
                .ifPresent(exec -> processBaseExecution(brokerAPIClient, clientConfig, stdDev.get(), algoConfig.stdDevMultiplier(), exec, stockQuote, algoConfig, market));
    }

    private StockQuote getStockQuote(String code, IBrokerAPIClient brokerAPIClient) {
        try {
            if (ExchangeCode.isUSStockCode(code)) {
                return webParserRestService.getRealTimeQuotes(List.of(code)).get().getBody()[0];
            }

            return brokerAPIClient.getStockQuote(code);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<Double> getStdDev(String code, int stdDevRange) {
        return dailyAssetSummaryRepo.findTopBySymbolOrderByDateDesc(code)
                .flatMap(summary -> Optional.ofNullable(summary.stdDevs.get(stdDevRange)));
    }

    private void processBaseExecution(IBrokerAPIClient brokerAPIClient, FutuClientConfig config, double stdDev, double stdDevMultiplier, Execution execution, StockQuote stockQuote, AlgoConfig algoConfig, ExchangeCode.Market market) {
        log.info("base price: {}", execution.price);    // used in test case
        log.info("process base execution: {}", execution);

        var pendingOrders = brokerAPIClient.getPendingOrders(market);
        var anyPartialFilledOrder = pendingOrders.stream()
                .filter(o -> algoConfig.code().equals(o.code()))
                .filter(Order::isPartialFilled).findFirst();
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

        int buyQuantity = algoConfig.quantity() > 0 ? algoConfig.quantity() : execution.quantity;
        handleOrderForBaseExecution(BUY, execution, pendingOrders, stdDev, stdDevMultiplier, brokerAPIClient, config, stockQuote, buyQuantity);
        handleOrderForBaseExecution(SELL, execution, pendingOrders, stdDev, stdDevMultiplier, brokerAPIClient, config, stockQuote, execution.quantity);
    }

    private void handleOrderForBaseExecution(Side pendingOrderSide, Execution baseExec, List<Order> pendingOrders, double stdDev, double stdDevMultiplier, IBrokerAPIClient brokerAPIClient, FutuClientConfig clientConfig, StockQuote stockQuote, int quantity) {
        log.info("handle {} order", pendingOrders);
        if (pendingOrderSide == SELL && baseExec.side == SELL) return;

        var stockCode = baseExec.code;
        var targetPrice = calculateTargetPrice(pendingOrderSide, stockCode, baseExec, stdDev, stdDevMultiplier, Double.parseDouble(stockQuote.getPrice()));
        var matchedPendingOrders = pendingOrders.stream()
                .filter(o -> o.side() == pendingOrderSide &&  o.code().equals(baseExec.code))
                .toList();
        if (matchedPendingOrders.size() > 1) {
            matchedPendingOrders.forEach(o -> {
                cancelOrder(brokerAPIClient, o.orderId(), stockCode);
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
            cancelOrder(brokerAPIClient, pendingOrderId, stockCode);
            telegramAPIClient.sendMessage(String.format("Cancelled order (%s): %s %s %s@%.2f", clientConfig.fundName(),
                    pendingOrder.side(), stockCode,pendingOrder.quantity(), pendingOrderPrice));
        }

        placeOrder(brokerAPIClient, clientConfig, stockCode, pendingOrderSide, targetPrice, quantity);
    }

    private double calculateTargetPrice(Side orderSide, String code, Execution baseExec, double stdDev, double stdDevMultiplier, double marketPrice) {
        var basePrice = baseExec.price;
        var modifiedStdDevPercentage = Math.min((stdDev * stdDevMultiplier / 100), 0.02618); // 0.01618 ^ 2
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

        var tickSize = tickSizes.getOrDefault(code, 0.01);  // default US to 0.01
        targetPrice = orderSide == BUY ? Math.floor(targetPrice / tickSize) * tickSize : Math.ceil(targetPrice / tickSize) * tickSize;
        targetPrice = (double) Math.round(targetPrice * 1000) / 1000;
        log.info("{}: targetPrice={}, basePrice={}, stdDev={}, mktPx={}", orderSide, targetPrice, basePrice, stdDev, marketPrice);
        return targetPrice;
    }

    private void placeOrder(IBrokerAPIClient brokerAPIClient, FutuClientConfig config, String stockCode, Side side, double price, int quantity) {
        var placeOrderResponse = brokerAPIClient.placeOrder(side, stockCode, quantity, price);

        if (placeOrderResponse.errorCode() > 0) {
            log.error("Cannot place order, error cod={}, message={}", placeOrderResponse.errorCode(), placeOrderResponse.message());
            throw new RuntimeException("Cannot place order");
        }

        String placedMessage = String.format("Placed order (%s): %s %s %d@%.2f",
                config.fundName(),
                side, stockCode, quantity, price);
        log.info(placedMessage);
        telegramAPIClient.sendMessage(placedMessage);
    }

    private void cancelOrder(IBrokerAPIClient brokerAPIClient, long pendingOrderId, String code) {
        var cancelOrderResponse = brokerAPIClient.cancelOrder(pendingOrderId, code);

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
        double tolerance = Math.max(Math.abs(p1), Math.abs(p2)) * priceThresholdPercent;
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
}
