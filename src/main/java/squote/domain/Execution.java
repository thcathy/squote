package squote.domain;

import squote.SquoteConstants;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.StringJoiner;

public class Execution {
    private String orderId;
    private BigDecimal price;
    private BigDecimal quantity;
    private BigDecimal quoteQuantity;
    private long time;
    private String code;
    private ExchangeCode.Market market;
    private SquoteConstants.Side side;
    private String fillIds;

    @Override
    public String toString() {
        return new StringJoiner(", ", Execution.class.getSimpleName() + "[", "]")
                .add(orderId + ":" + fillIds)
                .add(side + " " + code + ".").add(market == null ? ExchangeCode.Market.HK.toString() : market.toString())
                .add(quantity + "@" + price)
                .add("quoteQuantity=" + quoteQuantity)
                .add("time=" + time)
                .toString();
    }

    public Execution addExecution(Execution next) {
        price = getValue().add(next.getValue()).divide(quantity.add(next.quantity), RoundingMode.HALF_UP);
        quantity = quantity.add(next.quantity);
        time = Math.max(time, next.time);
        fillIds += "," + next.fillIds;
        return this;
    }

    public BigDecimal getValue() { return quantity.multiply(price); }

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

    public String getCode() {
        return code;
    }

    public Execution setCode(String code) {
        this.code = code;
        return this;
    }

    public SquoteConstants.Side getSide() {
        return side;
    }

    public Execution setSide(SquoteConstants.Side side) {
        this.side = side;
        return this;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getFillIds() {
        return fillIds;
    }

    public void setFillIds(String fillIds) {
        this.fillIds = fillIds;
    }

    public ExchangeCode.Market getMarket() {
        return market;
    }

    public void setMarket(ExchangeCode.Market market) {
        this.market = market;
    }
}
