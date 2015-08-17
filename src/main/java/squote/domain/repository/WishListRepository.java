package squote.domain.repository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import squote.domain.WishList;

@Repository
public interface WishListRepository extends PagingAndSortingRepository<WishList, String> {
	
}
