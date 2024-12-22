package squote.domain;

import squote.SquoteConstants;

import java.util.Date;

import static squote.SquoteConstants.Side.BUY;

public record Order(String code, SquoteConstants.Side side, int quantity, double price, long orderId,
                    int filledQuantity, double filledAveragePrice, Date createdAt) {

    public static Order newOrder(String code, SquoteConstants.Side side, int quantity, double price, long orderId) {
        return new Order(code, BUY, 4000, price, orderId, 0, 0, new Date());
    }
}
