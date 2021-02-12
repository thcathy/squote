package squote.domain;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import squote.IntegrationTest;
import squote.domain.repository.StockQueryRepository;

import java.util.Optional;

import static org.springframework.test.util.AssertionErrors.assertFalse;

public class StockQueryIntegrationTest extends IntegrationTest {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired StockQueryRepository stockQueryRepo;

	String userId = "StockQueryIntegrationTestUser";
	
    @Test
    public void saveToMongo_GivenBigDecimal_ShouldKeepCorrectFormat() {
    	final String queryStr = "abcde";
    	StockQuery q = new StockQuery(userId, queryStr);
    	stockQueryRepo.save(q);
    	
    	StockQuery q2 = stockQueryRepo.findByUserId(userId).get();
    	log.debug("StockQuery obj found: {}", q2);
    	assert queryStr.equals(q2.getDelimitedStocks());    	
    	    	
    	stockQueryRepo.delete(q2);
    }
    
    @Test
    public void findByKey_GivenNonexistKey_ShouldReturnNull() {
    	Optional<StockQuery> q = stockQueryRepo.findByUserId("unknown user");
    	log.debug("nonexist stock query: {}", q);
    	assertFalse("no stock query return when using wrong user id", q.isPresent());
    }
}
