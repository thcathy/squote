package squote.domain.repository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import squote.domain.HoldingStock;

@Repository
public interface HoldingStockRepository extends PagingAndSortingRepository<HoldingStock, String> {
	
}
