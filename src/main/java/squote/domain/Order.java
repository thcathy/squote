package squote.domain;

import squote.SquoteConstants;

import java.util.Date;

public record Order(String code, SquoteConstants.Side side, int quantity, double price, long orderId,
                    int filledQuantity, double filledAveragePrice, Date createdAt) {

    public static Order newOrder(String code, SquoteConstants.Side side, int quantity, double price, long orderId) {
        return new Order(code, side, quantity, price, orderId, 0, 0, new Date());
    }

    public boolean isPartialFilled() {
        return filledQuantity > 0 && filledQuantity < quantity;
    }
}
