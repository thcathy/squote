package squote;

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
import squote.service.CentralWebQueryService;
import squote.service.MarketReportService;
import squote.service.StockPerformanceService;
import squote.service.UpdateFundByHoldingService;

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
	@Value("${adminstrator.email}") 		private String adminEmail;
	@Value("${application.email}")			private String appEmail;
	@Value("${centralWebQuery.pool.size}")	private int poolSize;
	
	// repository interface
	@Autowired private HoldingStockRepository holdingStockRepo;
	@Autowired private MarketDailyReportRepository marketDailyReportRepo;
	@Autowired private FundRepository fundRepo;

	// Serivce Beans
	@Bean
	public CentralWebQueryService centrolWebQueryService() {
		return new CentralWebQueryService(poolSize);
	}

	@Bean
	public MarketReportService marketReportService() {
		return new MarketReportService(marketDailyReportRepo,
				centrolWebQueryService());
	}

	@Bean
	public StockPerformanceService stockPerformanceService() {
		return new StockPerformanceService(centrolWebQueryService().getExecutor());
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