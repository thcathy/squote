package squote.domain.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import squote.domain.DailyAssetSummary;

@Repository
public interface DailyAssetSummaryRepository extends CrudRepository<DailyAssetSummary, String> {
}
