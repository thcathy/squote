package squote;

import javax.servlet.Filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.filter.CharacterEncodingFilter;

import squote.domain.repository.HoldingStockRepository;
import squote.domain.repository.MarketDailyReportRepository;
import squote.service.CentralWebQueryService;
import squote.service.CheckWebService;
import squote.service.MarketReportService;
import squote.service.StockPerformanceService;

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
	@Value("${checkweb.url.list}")			private String checkWebUrlList;
	@Value("${adminstrator.email}") 		private String adminEmail;
	@Value("${application.email}")			private String appEmail;
	@Value("${smtp.username}")				private String smtpUsername;
	@Value("${smtp.password}")				private String smtpPassword;
	@Value("${centralWebQuery.pool.size}")	private int poolSize;

	// repository interface
	@Autowired
	private HoldingStockRepository holdingStockRepo;
	@Autowired
	private MarketDailyReportRepository marketDailyReportRepo;

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
	public CheckWebService checkWebService() {
		return new CheckWebService(checkWebUrlList.split(","), adminEmail,
				appEmail, smtpUsername, smtpPassword);
	}
		
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(SpringQuoteWebApplication.class);
	}	

	// Schedule jobs

	@Scheduled(fixedDelay = 60000)
	public void checkWebs() {
		checkWebService().check();
	}

	@Bean
	public Filter characterEncodingFilter() {
		CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();
		characterEncodingFilter.setEncoding("UTF-8");
		characterEncodingFilter.setForceEncoding(true);
		return characterEncodingFilter;
	}

	/**
	 * Main function for the whole application
	 */
	public static void main(String[] args) throws Exception {
		SpringApplication.run(SpringQuoteWebApplication.class, args);
	}

}