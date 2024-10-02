package com.axer.es.account.domain.projection;

import com.axer.es.account.domain.entities.Account;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

@Repository
public interface AccountRepository extends CrudRepository<Account, Long> {
    Account findByAggregateId(Long aggregateId);
}
