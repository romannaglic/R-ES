package com.r.es.account.domain.repository;

import com.r.data.jpa.entities.EntityId;
import com.r.data.jpa.entities.EventRecord;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

@Repository
public interface EventRecordCrudRepository extends CrudRepository<EventRecord, EntityId> {
}
