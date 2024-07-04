package squote.domain.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import squote.domain.HoldingStock;

import java.util.List;

@Repository
public interface HoldingStockRepository extends PagingAndSortingRepository<HoldingStock, String>, CrudRepository<HoldingStock, String> {
	List<HoldingStock> findByUserIdOrderByDate(String userId);
}
