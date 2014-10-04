 package squote.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import squote.domain.HoldingStock;
import squote.domain.StockExecutionMessage;
import squote.domain.StockQuote;
import squote.domain.repository.HoldingStockRepository;
import squote.web.parser.AastockStockQuoteParser;
import squote.web.parser.EtnetIndexQuoteParser;

public class QuoteService {
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	public static final String CODE_SEPARATOR = ",";
	
	private final CentralWebQueryService executeService;
	private final HoldingStockRepository holdingStockRepo;
		
	public QuoteService(CentralWebQueryService executeService,
			HoldingStockRepository holdingStockRepo) {
		super();
		this.executeService = executeService;
		this.holdingStockRepo = holdingStockRepo;
	}

	public List<Future<StockQuote>> getStockQuoteList(String codeList) {
		String[] codes = codeList.split(CODE_SEPARATOR);
		List<Future<StockQuote>> futures = new ArrayList<Future<StockQuote>>();
		for (String code : codes) futures.add(executeService.submit(new AastockStockQuoteParser(code)));				
    	return futures;
	}
	
	public Future<List<StockQuote>> getGlobalIndexes() {
		return executeService.submit(new Callable<List<StockQuote>>() {
			public List<StockQuote> call() throws Exception {				
				return new EtnetIndexQuoteParser().getGlobalIndexes();
			}			
		});
	}
	
	public Future<Map<HoldingStock, StockQuote>> getAllHoldingStocksWithMapping() {
		return executeService.submit(new Callable<Map<HoldingStock, StockQuote>>() {
			public Map<HoldingStock, StockQuote> call() throws Exception {
				// main body of business logic 
				Map<HoldingStock, StockQuote> resultMap = new HashMap<HoldingStock, StockQuote>();
				
				for (HoldingStock holding : holdingStockRepo.findAll()) {
					StockQuote quote = new AastockStockQuoteParser(holding.getCode()).getStockQuote();
					resultMap.put(holding, quote);
				}		
				
				return resultMap;
			}
		});
	}	
	
	public HoldingStock createHoldingStocksFromExecution(StockExecutionMessage message, BigDecimal hscei) {
		HoldingStock s = new HoldingStock();
		s.setCode(message.getCode());
		s.setQuantity(message.getQuantity());
		s.setGross(new BigDecimal(message.getPrice() * message.getQuantity()));
		s.setDate(message.getDate());
		s.setSide(message.getSide());
		s.setHsce(hscei);
		
		holdingStockRepo.save(s);
		return s;
	}
}
