package squote.service.yahoo;

/**
 * Java representation of Yahoo Finance Ticker protobuf message
 * Based on the protobuf schema found in various GitHub repositories
 */
public class YahooTicker {
    private String id;
    private float price;
    private long time;
    private String currency;
    private String exchange;
    private QuoteType quoteType;
    private MarketHours marketHours;
    private float changePercent;
    private long dayVolume;
    private float dayHigh;
    private float dayLow;
    private float change;
    private String shortName;
    private long expireDate;
    private float openPrice;
    private float previousClose;
    private float strikePrice;
    private String underlyingSymbol;
    private long openInterest;
    private int optionsType;
    private long miniOption;
    private long lastSize;
    private float bid;
    private long bidSize;
    private float ask;
    private long askSize;
    private long priceHint;
    private long vol24hr;
    private long volAllCurrencies;
    private String fromCurrency;
    private String lastMarket;
    private float circulatingSupply;
    private float marketCap;

    // Enums based on Yahoo Finance protobuf schema
    public enum QuoteType {
        EQUITY(8),
        OPTION(9),
        INDEX(10),
        CURRENCY(11),
        CRYPTOCURRENCY(12),
        ETF(13),
        FUTURE(14),
        MUTUALFUND(15);

        private final int value;

        QuoteType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static QuoteType fromValue(int value) {
            for (QuoteType type : values()) {
                if (type.value == value) {
                    return type;
                }
            }
            return EQUITY; // default
        }
    }

    public enum MarketHours {
        PRE_MARKET(0),
        REGULAR_MARKET(1),
        POST_MARKET(2),
        EXTENDED_HOURS_MARKET(3);

        private final int value;

        MarketHours(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static MarketHours fromValue(int value) {
            for (MarketHours hours : values()) {
                if (hours.value == value) {
                    return hours;
                }
            }
            return REGULAR_MARKET; // default
        }
    }

    // Constructors
    public YahooTicker() {}

    public YahooTicker(String id, float price, long time) {
        this.id = id;
        this.price = price;
        this.time = time;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public float getPrice() { return price; }
    public void setPrice(float price) { this.price = price; }

    public long getTime() { return time; }
    public void setTime(long time) { this.time = time; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }

    public QuoteType getQuoteType() { return quoteType; }
    public void setQuoteType(QuoteType quoteType) { this.quoteType = quoteType; }

    public MarketHours getMarketHours() { return marketHours; }
    public void setMarketHours(MarketHours marketHours) { this.marketHours = marketHours; }

    public float getChangePercent() { return changePercent; }
    public void setChangePercent(float changePercent) { this.changePercent = changePercent; }

    public long getDayVolume() { return dayVolume; }
    public void setDayVolume(long dayVolume) { this.dayVolume = dayVolume; }

    public float getDayHigh() { return dayHigh; }
    public void setDayHigh(float dayHigh) { this.dayHigh = dayHigh; }

    public float getDayLow() { return dayLow; }
    public void setDayLow(float dayLow) { this.dayLow = dayLow; }

    public float getChange() { return change; }
    public void setChange(float change) { this.change = change; }

    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }

    public long getExpireDate() { return expireDate; }
    public void setExpireDate(long expireDate) { this.expireDate = expireDate; }

    public float getOpenPrice() { return openPrice; }
    public void setOpenPrice(float openPrice) { this.openPrice = openPrice; }

    public float getPreviousClose() { return previousClose; }
    public void setPreviousClose(float previousClose) { this.previousClose = previousClose; }

    public float getStrikePrice() { return strikePrice; }
    public void setStrikePrice(float strikePrice) { this.strikePrice = strikePrice; }

    public String getUnderlyingSymbol() { return underlyingSymbol; }
    public void setUnderlyingSymbol(String underlyingSymbol) { this.underlyingSymbol = underlyingSymbol; }

    public long getOpenInterest() { return openInterest; }
    public void setOpenInterest(long openInterest) { this.openInterest = openInterest; }

    public int getOptionsType() { return optionsType; }
    public void setOptionsType(int optionsType) { this.optionsType = optionsType; }

    public long getMiniOption() { return miniOption; }
    public void setMiniOption(long miniOption) { this.miniOption = miniOption; }

    public long getLastSize() { return lastSize; }
    public void setLastSize(long lastSize) { this.lastSize = lastSize; }

    public float getBid() { return bid; }
    public void setBid(float bid) { this.bid = bid; }

    public long getBidSize() { return bidSize; }
    public void setBidSize(long bidSize) { this.bidSize = bidSize; }

    public float getAsk() { return ask; }
    public void setAsk(float ask) { this.ask = ask; }

    public long getAskSize() { return askSize; }
    public void setAskSize(long askSize) { this.askSize = askSize; }

    public long getPriceHint() { return priceHint; }
    public void setPriceHint(long priceHint) { this.priceHint = priceHint; }

    public long getVol24hr() { return vol24hr; }
    public void setVol24hr(long vol24hr) { this.vol24hr = vol24hr; }

    public long getVolAllCurrencies() { return volAllCurrencies; }
    public void setVolAllCurrencies(long volAllCurrencies) { this.volAllCurrencies = volAllCurrencies; }

    public String getFromCurrency() { return fromCurrency; }
    public void setFromCurrency(String fromCurrency) { this.fromCurrency = fromCurrency; }

    public String getLastMarket() { return lastMarket; }
    public void setLastMarket(String lastMarket) { this.lastMarket = lastMarket; }

    public float getCirculatingSupply() { return circulatingSupply; }
    public void setCirculatingSupply(float circulatingSupply) { this.circulatingSupply = circulatingSupply; }

    public float getMarketCap() { return marketCap; }
    public void setMarketCap(float marketCap) { this.marketCap = marketCap; }

    @Override
    public String toString() {
        return String.format("YahooTicker{id='%s', price=%.2f, time=%d, exchange='%s', changePercent=%.2f%%}",
                id, price, time, exchange, changePercent);
    }
} 
