package squote.service;

import com.binance.api.client.BinanceApiAsyncRestClient;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.account.Trade;
import com.binance.api.client.domain.market.TickerPrice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import squote.domain.Execution;
import squote.domain.StockQuote;
import squote.domain.repository.FundRepository;
import squote.domain.repository.HoldingStockRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static squote.SquoteConstants.Side.BUY;
import static squote.SquoteConstants.Side.SELL;

public class BinanceAPIService {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired FundRepository fundRepo;
	@Autowired HoldingStockRepository holdingRepo;

	private final BinanceApiRestClient syncClient;
	private final BinanceApiAsyncRestClient asyncClient;


	public BinanceAPIService(String apiKey, String secret) {
		BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance(apiKey, secret);
		syncClient = factory.newRestClient();
		asyncClient = factory.newAsyncRestClient();
	}
	
	public List<Execution> getMyTrades(String code) {
		List<Trade> trades = syncClient.getMyTrades(code);
		log.info("My trade from {}: return  {}", code, trades);
		return trades.stream().map(this::toExecution).collect(Collectors.toList());
	}

	private Execution toExecution(Trade trade) {
		return new Execution()
				.setPrice(new BigDecimal(trade.getPrice()))
				.setQuantity(new BigDecimal(trade.getQty()))
				.setQuoteQuantity(new BigDecimal(trade.getQuoteQty()))
				.setSide(trade.isBuyer() ? BUY : SELL)
				.setSymbol(trade.getSymbol())
				.setTime(trade.getTime());
	}


	public Map<String, StockQuote> getAllPrices() {
		return syncClient.getAllPrices().stream()
				.map(this::toStockQuote)
				.collect(Collectors.toMap(StockQuote::getStockCode, v -> v));
	}

	private StockQuote toStockQuote(TickerPrice tickerPrice) {
		return new StockQuote(tickerPrice.getSymbol()).setPrice(tickerPrice.getPrice());
	}

}
    
