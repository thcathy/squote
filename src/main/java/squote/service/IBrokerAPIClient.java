package squote.service;

import squote.SquoteConstants;
import squote.domain.Execution;
import squote.domain.Market;
import squote.domain.Order;
import squote.domain.StockQuote;

import java.util.List;
import java.util.Map;

public interface IBrokerAPIClient {
    List<Order> getPendingOrders(Market market);
    StockQuote getStockQuote(String code);
    Map<String, Execution> getStockTodayExecutions(Market market);
    PlaceOrderResponse placeOrder(SquoteConstants.Side side, String code, int quantity, double price);
    CancelOrderResponse cancelOrder(long orderId, String code);

    record PlaceOrderResponse(long orderId, long errorCode, String message) {}
    record CancelOrderResponse(long errorCode, String message) {}
}
