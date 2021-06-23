package squote.domain;

import squote.SquoteConstants;

import java.math.BigDecimal;
import java.util.StringJoiner;

public class Execution {
    private BigDecimal price;
    private BigDecimal quantity;
    private BigDecimal quoteQuantity;
    private long time;
    private String symbol;
    private SquoteConstants.Side side;

    @Override
    public String toString() {
        return new StringJoiner(", ", Execution.class.getSimpleName() + "[", "]")
                .add("symbol='" + symbol + "'")
                .add("side=" + side)
                .add("price=" + price)
                .add("quantity=" + quantity)
                .add("quoteQuantity=" + quoteQuantity)
                .add("time=" + time)
                .toString();
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Execution setPrice(BigDecimal price) {
        this.price = price;
        return this;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public Execution setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
        return this;
    }

    public BigDecimal getQuoteQuantity() {
        return quoteQuantity;
    }

    public Execution setQuoteQuantity(BigDecimal quoteQuantity) {
        this.quoteQuantity = quoteQuantity;
        return this;
    }

    public long getTime() {
        return time;
    }

    public Execution setTime(long time) {
        this.time = time;
        return this;
    }

    public String getSymbol() {
        return symbol;
    }

    public Execution setSymbol(String symbol) {
        this.symbol = symbol;
        return this;
    }

    public SquoteConstants.Side getSide() {
        return side;
    }

    public Execution setSide(SquoteConstants.Side side) {
        this.side = side;
        return this;
    }
}
