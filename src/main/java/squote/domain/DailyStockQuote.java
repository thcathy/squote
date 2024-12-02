package squote.domain;

import java.util.Date;

public record DailyStockQuote(Date date, double open, double close, double high, double low, int volume, double adjClose) {}
