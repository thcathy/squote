package squote.domain.repository;
import java.math.BigInteger;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import squote.domain.HoldingStock;

@Repository
public interface HoldingStockRepository extends PagingAndSortingRepository<HoldingStock, BigInteger> {
	
}
