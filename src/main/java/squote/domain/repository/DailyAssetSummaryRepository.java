package squote.domain.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import squote.domain.DailyAssetSummary;

import java.util.Optional;

@Repository
public interface DailyAssetSummaryRepository extends CrudRepository<DailyAssetSummary, String> {
    Optional<DailyAssetSummary> findTopBySymbolOrderByDateDesc(String symbol);
}
