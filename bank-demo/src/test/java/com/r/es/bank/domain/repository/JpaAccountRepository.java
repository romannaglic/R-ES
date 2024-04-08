package com.r.es.bank.domain.repository;

import com.r.es.bank.domain.entities.Account;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;

@Repository
public interface JpaAccountRepository extends CrudRepository<Account, Long> {
    Optional<Account> findByAggregateId(Long aggregateId);

    Optional<Account> retrieveByAccountNumber(String accountNumber);

    Optional<Long> findAggregateIdByAccountNumber(String accountNumber);

    Optional<Account> findByAccountNumber(String accountNumber);

}
