package squote.service;

import com.binance.api.client.BinanceApiAsyncRestClient;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.account.Trade;
import com.binance.api.client.domain.market.TickerPrice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import squote.domain.repository.FundRepository;
import squote.domain.repository.HoldingStockRepository;

import java.util.List;

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
	
	public List<Trade> getMyTrades(String code) {
		List<Trade> trades = syncClient.getMyTrades(code);
		log.info("My trade from {}: return  {}", code, trades);
		return trades;
	}

	public List<TickerPrice> getAllPrices() {
		return syncClient.getAllPrices();
	}
	
}
    
