package squote.domain.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import squote.domain.StockQuery;

import java.math.BigInteger;
import java.util.Optional;

@Repository
public interface StockQueryRepository extends MongoRepository<StockQuery, BigInteger> {
    Optional<StockQuery> findByUserId(String userId);
}
