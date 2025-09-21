package squote.service;

import squote.SquoteConstants;
import squote.domain.Execution;
import squote.domain.Market;
import squote.domain.Order;
import squote.domain.StockQuote;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface IBrokerAPIClient {
    List<Order> getPendingOrders(Market market);
    StockQuote getStockQuote(String code);
    Map<String, Execution> getStockTodayExecutions(Market market);
    PlaceOrderResponse placeOrder(SquoteConstants.Side side, String code, int quantity, double price);
    CancelOrderResponse cancelOrder(long orderId, String code);
    
    /**
     * Get recent executions (implementation-dependent time range)
     * IB: T-day only
     * Futu: Same as historical
     * @param fromDate the starting date/time to retrieve executions from
     * @param market the market to query executions for
     * @return map of executions keyed by order ID
     */
    Map<String, Execution> getRecentExecutions(Date fromDate, Market market);
    
    /**
     * Get historical executions if supported
     * @param fromDate the starting date to retrieve executions from
     * @param market the market to query executions for
     * @return map of executions keyed by order ID
     * @throws UnsupportedOperationException if not supported by broker
     */
    Map<String, Execution> getHistoricalExecutions(Date fromDate, Market market);

    record PlaceOrderResponse(long orderId, long errorCode, String message) {}
    record CancelOrderResponse(long errorCode, String message) {}


}
