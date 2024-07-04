package squote.domain.repository;

 import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import squote.domain.Fund;

import java.util.List;
import java.util.Optional;

@Repository
public interface FundRepository extends
		PagingAndSortingRepository<Fund, String>,
		CrudRepository<Fund, String> {
	List<Fund> findByUserId(String userId);
	Optional<Fund> findByUserIdAndName(String userId, String name);
}
