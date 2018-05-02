package squote.domain.repository;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import squote.domain.VisitedForumThread;

import java.util.List;

@Repository
public interface VisitedForumThreadRepository extends PagingAndSortingRepository<VisitedForumThread, String> {

    List<VisitedForumThread> findByTitle(String title);
}
