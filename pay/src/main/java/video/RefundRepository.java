package video;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="refunds", path="refunds")
public interface RefundRepository extends PagingAndSortingRepository<Refund, Long>{


}
