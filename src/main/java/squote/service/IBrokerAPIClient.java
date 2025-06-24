package squote.service;

import squote.SquoteConstants;
import squote.domain.Execution;
import squote.domain.Order;
import squote.domain.StockQuote;

import java.util.List;
import java.util.Map;

public interface IBrokerAPIClient {
    List<Order> getPendingOrders();
    StockQuote getStockQuote(String code);
    Map<String, Execution> getHKStockTodayExecutions();
    PlaceOrderResponse placeOrder(SquoteConstants.Side side, String code, int quantity, double price);
    CancelOrderResponse cancelOrder(long orderId);

    record PlaceOrderResponse(long orderId, long errorCode, String message) {}
    record CancelOrderResponse(long errorCode, String message) {}
}
