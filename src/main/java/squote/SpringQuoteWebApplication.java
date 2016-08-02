package squote;

import com.mashape.unirest.http.Unirest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import squote.domain.repository.FundRepository;
import squote.domain.repository.HoldingStockRepository;
import squote.domain.repository.MarketDailyReportRepository;
import squote.service.*;
import squote.unirest.UnirestSetup;

import javax.annotation.PostConstruct;
import javax.servlet.Filter;

@Configuration
@EnableAutoConfiguration
@ComponentScan
@EnableScheduling
public class SpringQuoteWebApplication extends SpringBootServletInitializer {

	@Configuration
	@PropertySource("classpath:application.properties")
	static class Default {}

	@Configuration
	@Profile("dev")
	@PropertySource({"classpath:application.properties", "classpath:application-dev.properties"})
	static class Dev {}

	// application properties	
	@Value("${adminstrator.email}") 				String adminEmail;
	@Value("${application.email}")					String appEmail;
	@Value("${centralWebQuery.pool.size}")			int poolSize;
	@Value("${http.max_connection:20}") 			int httpMaxConnection;
	@Value("${http.max_connection_per_route:20}") 	int getHttpMaxConnectionPerRoute;
	@Value("${apiserver.host}")						String APIServerHost;
	
	// repository interface
	@Autowired private HoldingStockRepository holdingStockRepo;
	@Autowired private MarketDailyReportRepository marketDailyReportRepo;
	@Autowired private FundRepository fundRepo;

	@PostConstruct
	public void configure() {
		UnirestSetup.setupAll();
		Unirest.setConcurrency(httpMaxConnection,getHttpMaxConnectionPerRoute);
	}

	// Serivce Beans
	@Bean
	public CentralWebQueryService centralWebQueryService() {
		return new CentralWebQueryService(poolSize);
	}

	@Bean
	public WebParserRestService webParserRestService() { return new WebParserRestService(APIServerHost); }

	@Bean
	public MarketReportService marketReportService() {
		return new MarketReportService(marketDailyReportRepo,
				centralWebQueryService());
	}

	@Bean
	public StockPerformanceService stockPerformanceService() {
		return new StockPerformanceService(centralWebQueryService().getExecutor());
	}
			
	@Bean
	public Filter characterEncodingFilter() {
		CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();
		characterEncodingFilter.setEncoding("UTF-8");
		characterEncodingFilter.setForceEncoding(true);
		return characterEncodingFilter;
	}
	
	@Bean
	public UpdateFundByHoldingService updateFundByHoldingService() {
		return new UpdateFundByHoldingService(fundRepo, holdingStockRepo);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(SpringQuoteWebApplication.class);
	}	
	
	@Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurerAdapter() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/rest/**");
            }
        };
    }
	
	/**
	 * Main function for the whole application
	 */
	public static void main(String[] args) throws Exception {
		ConfigurableApplicationContext c = SpringApplication.run(SpringQuoteWebApplication.class, args);
	}

}