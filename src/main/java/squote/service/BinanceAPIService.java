package squote.service;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.binance.BinanceExchange;
import org.knowm.xchange.binance.dto.trade.BinanceTradeHistoryParams;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.service.marketdata.MarketDataService;
import org.knowm.xchange.service.trade.TradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import squote.domain.Execution;
import squote.domain.StockQuote;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.knowm.xchange.currency.CurrencyPair.BTC_USDT;
import static squote.SquoteConstants.Side.BUY;
import static squote.SquoteConstants.Side.SELL;

public class BinanceAPIService {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final TradeService tradeService;
	private final MarketDataService marketDataService;

	// for testing
	public BinanceAPIService() {
		tradeService = null;
		marketDataService = null;
	}

	public BinanceAPIService(String apiKey, String secret) {
		ExchangeSpecification exSpec = new BinanceExchange().getDefaultExchangeSpecification();
		exSpec.setApiKey(apiKey);
		exSpec.setSecretKey(secret);
		Exchange exchange = ExchangeFactory.INSTANCE.createExchange(exSpec);
		tradeService = exchange.getTradeService();
		marketDataService = exchange.getMarketDataService();
	}

	public List<Execution> getMyTrades(String code) {
        try {
            var trades = tradeService.getTradeHistory(new BinanceTradeHistoryParams()).getTrades();
			log.info("My trades for {}: {}", code, trades);
			return trades.stream().map(this::toExecution).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
	}

	private Execution toExecution(Trade trade) {
		return new Execution()
				.setPrice(trade.getPrice())
				.setQuantity(trade.getOriginalAmount())
				.setQuoteQuantity(trade.getPrice().multiply(trade.getOriginalAmount()))
				.setSide(trade.getType() == Order.OrderType.BID ? BUY : SELL)
				.setCode(trade.getInstrument().toString())
				.setTime(trade.getTimestamp().getTime());
	}

	public Map<String, StockQuote> getAllPrices() {
		var pairs = List.of(BTC_USDT,
				new CurrencyPair("MANA", "USDT"),
				new CurrencyPair("DOGE", "USDT"),
				new CurrencyPair("UNI", "USDT"),
				new CurrencyPair("SAND", "USDT"),
				new CurrencyPair("DOT", "USDT"),
				new CurrencyPair("ADA", "USDT"),
				new CurrencyPair("SHIB", "USDT"));
		return pairs.stream().map(pair -> {
            try {
                return marketDataService.getTicker(pair);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
				return null;
            }
        }).filter(Objects::nonNull)
				.map(this::toStockQuote)
				.collect(Collectors.toMap(StockQuote::getStockCode, v -> v));
	}

	private StockQuote toStockQuote(Ticker ticker) {
		return new StockQuote(ticker.getInstrument().getBase().getCurrencyCode() + ticker.getInstrument().getCounter().getCurrencyCode())
				.setPrice(ticker.getLast().toPlainString());
	}
}
