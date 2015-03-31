package squote.web.parser;

import java.util.concurrent.Callable;

import squote.domain.StockQuote;

public interface StockQuoteParser {
	public StockQuote getStockQuote();
}
