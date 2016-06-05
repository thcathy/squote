package squote.web.parser;

import squote.domain.StockQuote;

import java.util.function.Supplier;

import static squote.SquoteConstants.NA;

public interface StockQuoteParser {
	StockQuote getStockQuote();

	default String parse(Supplier<String> f) {
		try {
			return f.get();
		} catch (Exception e) {
			return NA;
		}
	}
}
