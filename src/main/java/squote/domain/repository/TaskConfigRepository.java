package squote.domain.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import squote.domain.TaskConfig;

@Repository
public interface TaskConfigRepository extends CrudRepository<TaskConfig, String> {
}
