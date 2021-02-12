package squote;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import squote.domain.repository.FundRepository;
import squote.domain.repository.HoldingStockRepository;
import squote.domain.repository.MarketDailyReportRepository;
import squote.security.AuthenticationService;
import squote.service.MarketReportService;
import squote.service.StockPerformanceService;
import squote.service.UpdateFundByHoldingService;
import squote.service.WebParserRestService;
import squote.unirest.UnirestSetup;

@SpringBootApplication
public class SpringQuoteWebApplication {

	@Configuration
	@PropertySource("classpath:application.properties")
	static class Default {}

	@Configuration
	@Profile("dev")
	@PropertySource({"classpath:application.properties", "classpath:application-dev.properties"})
	static class Dev {}

	// application properties
	@Value("${http.max_connection:20}") 			int httpMaxConnection;
	@Value("${http.max_connection_per_route:20}") 	int httpMaxConnectionPerRoute;
	@Value("${http.timeout:300000}")				int httpTimeout;
	@Value("${APISERVER_HOST}")						String APIServerHost;
	
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
			
//	@Bean
//	public Filter characterEncodingFilter() {
//		CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();
//		characterEncodingFilter.setEncoding("UTF-8");
//		characterEncodingFilter.setForceEncoding(true);
//		return characterEncodingFilter;
//	}

	@Bean
	public AuthenticationService authenticationService() {
		return new AuthenticationService();
	}
	
	@Bean
	public UpdateFundByHoldingService updateFundByHoldingService() {
		return new UpdateFundByHoldingService(fundRepo, holdingStockRepo);
	}
	
	/**
	 * Main function for the whole application
	 */
	public static void main(String[] args) throws Exception {
		ConfigurableApplicationContext c = SpringApplication.run(SpringQuoteWebApplication.class, args);
		c.getBean(SpringQuoteWebApplication.class).init();
	}

}
