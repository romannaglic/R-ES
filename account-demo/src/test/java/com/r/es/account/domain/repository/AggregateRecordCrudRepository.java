package com.r.es.account.domain.repository;

import com.r.data.jpa.entities.AggregateRecord;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

@Repository
public interface AggregateRecordCrudRepository extends CrudRepository<AggregateRecord, Long> {
}
