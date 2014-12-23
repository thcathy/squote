package squote.domain.repository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import squote.domain.VisitedForumThread;

@Repository
public interface VisitedForumThreadRepository extends PagingAndSortingRepository<VisitedForumThread, String> {
	
}
