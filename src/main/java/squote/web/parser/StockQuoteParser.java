package squote.web.parser;

import java.util.concurrent.Callable;

import squote.domain.StockQuote;

public interface StockQuoteParser extends Callable<StockQuote> {
	public StockQuote getStockQuote();
}
