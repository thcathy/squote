package squote.domain.repository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import squote.domain.Fund;
import squote.domain.HoldingStock;

@Repository
public interface FundRepository extends PagingAndSortingRepository<Fund, String> {
	
}
