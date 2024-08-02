package squote;

import com.futu.openapi.FTAPI_Conn_Trd;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.*;
import squote.domain.repository.FundRepository;
import squote.domain.repository.HoldingStockRepository;
import squote.domain.repository.MarketDailyReportRepository;
import squote.security.AuthenticationService;
import squote.service.*;
import squote.unirest.UnirestSetup;

@SpringBootApplication
@Import({ SchedulingConfiguration.class })
public class SpringQuoteWebApplication {

	@Configuration
	@PropertySource("classpath:application.yml")
	static class Default {}

	@Configuration
	@Profile("dev")
	@PropertySource({"classpath:application.yml", "classpath:application-dev.yml"})
	static class Dev {}

	// application properties
	@Value("${http.max_connection:20}") 			int httpMaxConnection;
	@Value("${http.max_connection_per_route:20}") 	int httpMaxConnectionPerRoute;
	@Value("${http.timeout:300000}")				int httpTimeout;
	@Value("${apiserver.host}")						String APIServerHost;
	@Value("${binance.apikey}")						String binanceAPIKey;
	@Value("${binance.apisecret}")					String binanceAPISecret;
	
	// repository interface
	@Autowired private HoldingStockRepository holdingStockRepo;
	@Autowired private MarketDailyReportRepository marketDailyReportRepo;
	@Autowired private FundRepository fundRepo;

	public void init() {
		UnirestSetup.MAX_TOTAL_HTTP_CONNECTION = httpMaxConnection;
		UnirestSetup.MAX_HTTP_CONNECTION_PER_ROUTE = httpMaxConnectionPerRoute;
		UnirestSetup.HTTP_TIMEOUT = httpTimeout;
		UnirestSetup.setupAll();
	}

	// Serivce Beans

	@Bean
	public WebParserRestService webParserRestService() { return new WebParserRestService(APIServerHost); }

	@Bean
	public MarketReportService marketReportService() {
		return new MarketReportService(marketDailyReportRepo,
				webParserRestService());
	}

	@Bean
	public StockPerformanceService stockPerformanceService() {
		return new StockPerformanceService(webParserRestService());
	}

	@Bean
	public AuthenticationService authenticationService() {
		return new AuthenticationService();
	}

	@Bean
	public BinanceAPIService binanceAPIService() {
		return new BinanceAPIService(binanceAPIKey, binanceAPISecret);
	}
	
	@Bean
	public UpdateFundByHoldingService updateFundByHoldingService() {
		return new UpdateFundByHoldingService(fundRepo, holdingStockRepo, binanceAPIService());
	}

	@Bean
	public FTAPI_Conn_Trd FTAPIConnTrd() { return new FTAPI_Conn_Trd(); }

	@Bean
	public EmailService emailService() { return new EmailService(); }


	/**
	 * Main function for the whole application
	 */
	public static void main(String[] args) {
		ConfigurableApplicationContext c = SpringApplication.run(SpringQuoteWebApplication.class, args);
		c.getBean(SpringQuoteWebApplication.class).init();
	}

}
