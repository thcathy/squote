package squote.domain.repository;
import org.springframework.data.mongodb.repository.MongoRepository;

import squote.domain.MarketDailyReport;

public interface MarketDailyReportRepository extends MongoRepository<MarketDailyReport, Integer> {
    
	MarketDailyReport findByDate(int date);
}
