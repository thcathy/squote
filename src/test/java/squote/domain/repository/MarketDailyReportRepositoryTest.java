package squote.domain.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import squote.IntegrationTest;
import squote.domain.MarketDailyReport;
import squote.domain.StockQuote;

import static org.assertj.core.api.Assertions.assertThat;

public class MarketDailyReportRepositoryTest extends IntegrationTest {
	private final Logger log = LoggerFactory.getLogger(MarketDailyReportRepositoryTest.class);

	@Autowired MarketDailyReportRepository repo;

	@AfterEach
	public void clearAll() {
		repo.deleteAll();
	}
	
	@Test
	public void create_and_write_marketDailyReport() {
		int date = 20210101;
		StockQuote hsiQuote = new StockQuote("2800");
		hsiQuote.setPreviousPrice(1, 10.0);
		StockQuote hsceiQuote = new StockQuote("2828");
		MarketDailyReport report = new MarketDailyReport(date, hsiQuote, hsceiQuote);
		repo.save(report);

		assertThat(repo.findByDate(date).getDate()).isEqualTo(date);
	}

}
