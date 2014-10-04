package squote.domain.repository;
import java.math.BigInteger;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import squote.domain.StockQuery;

@Repository
public interface StockQueryRepository extends MongoRepository<StockQuery, BigInteger> {

    List<StockQuery> findAll();
    StockQuery findByKey(int key);
}
