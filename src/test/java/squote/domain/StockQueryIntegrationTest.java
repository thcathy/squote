package squote.domain;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import squote.SpringQuoteWebApplication;
import squote.domain.repository.StockQueryRepository;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringQuoteWebApplication.class)
@ActiveProfiles("dev")
public class StockQueryIntegrationTest {
	protected final Logger log = LoggerFactory.getLogger(getClass());	
	@Autowired StockQueryRepository stockQueryRepo;
	
    @Test
    public void testStockQueryRepoSave() {
    	final String queryStr = "abcde";
    	StockQuery q = new StockQuery(queryStr);
    	q.setKey(10);
    	stockQueryRepo.save(q);
    	
    	StockQuery q2 = stockQueryRepo.findByKey(10);
    	log.debug("StockQuery obj found: {}", q2);
    	assert queryStr.equals(q2.getStockList());    	
    	    	
    	stockQueryRepo.delete(q2);
    }
    
    @Test
    public void testStockQueryRepoFindByKey() {
    	StockQuery q = stockQueryRepo.findByKey(999999);
    	log.debug("nonexist stock query: {}", q);
    	assertTrue("Query should return null if the object is not exist",q == null);
    }
}
