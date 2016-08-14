package squote.service;

import com.mashape.unirest.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import squote.SquoteConstants;
import squote.domain.StockQuote;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StockPerformanceService {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private static int expireAfterHour = 1;
	private volatile List<StockQuote> stockPerformanceQuotes;
	private Calendar expireOn = Calendar.getInstance();
	private Optional<List<String>> constituents = Optional.empty();

	private final WebParserRestService webService;
	
	public StockPerformanceService(WebParserRestService webService) {
		super();
		this.webService = webService;
	}

	public synchronized List<StockQuote> getStockPerformanceQuotes() throws ExecutionException, InterruptedException {
		if (cached()) return stockPerformanceQuotes;

		constituents = Optional.of(constituents.orElseGet(this::getConstituents));
		log.debug("Total constituents: {}", constituents.get().size());

		List<String> codes = constituents.get();
		codes.add("2828");

		List<Future<HttpResponse<StockQuote>>> quoteFutures = submitFullQuotesRequest(codes);
		stockPerformanceQuotes = collectStockQuotes(quoteFutures);
		log.debug("Total quotes returned: {}", stockPerformanceQuotes.size());

		updateExpireTime();
		return stockPerformanceQuotes;
	}

	private List<StockQuote> collectStockQuotes(List<Future<HttpResponse<StockQuote>>> quoteFutures) {
		return quoteFutures.stream()
									.flatMap(this::collectResponse)
									.map(HttpResponse::getBody)
									.sorted((x,y)->Double.compare(x.getLastYearPercentage(),y.getLastYearPercentage()))
									.collect(Collectors.toList());
	}

	private void updateExpireTime() {
		expireOn = Calendar.getInstance();
		expireOn.add(Calendar.HOUR, expireAfterHour);
	}

	private List<Future<HttpResponse<StockQuote>>> submitFullQuotesRequest(List<String> codes) throws InterruptedException, ExecutionException {
		log.debug("Start getting full stock quotes: total {}", codes.size());

		return codes.stream()
				.map(webService::getFullQuote)
				.collect(Collectors.toList());
	}

	private Stream<HttpResponse<StockQuote>> collectResponse(Future<HttpResponse<StockQuote>> future) {
		try {
			return Stream.of(future.get());
		} catch (Exception e) {
			log.debug("Cannot process http respsonse", e);
		}
		return Stream.empty();
	}

	private List<String> getConstituents() {
		log.debug("Start getting all index constituents");

		List<Future<HttpResponse<String[]>>> futures = Arrays.stream(SquoteConstants.IndexCode.values())
				.map(i -> webService.getConstituents(i.toString()))
				.collect(Collectors.toList());

		return futures.stream()
				.map(f -> thc.util.ConcurrentUtils.collect(f).getBody())
				.flatMap(Arrays::stream)
				.distinct()
				.collect(Collectors.toList());
	}

	private boolean cached() {
		return stockPerformanceQuotes != null && expireOn.getTime().compareTo(new Date()) > 0;
	}


}

