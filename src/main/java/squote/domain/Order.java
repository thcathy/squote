package squote.domain;

import squote.SquoteConstants;

import java.util.Date;

public record Order(String code, SquoteConstants.Side side, int quantity, double price, String orderId,
                    int filledQuantity, double filledAveragePrice, Date createdAt) {

}
