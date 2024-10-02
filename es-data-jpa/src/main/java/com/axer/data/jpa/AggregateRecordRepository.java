/*
 * Copyright 2017-2022 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.axer.data.jpa;

import com.axer.data.jpa.entities.AggregateRecord;
import com.axer.data.jpa.entities.EntityId;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.GenericRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;

/**
 * SnapshotsRepository.
 */
@Repository
public interface AggregateRecordRepository extends GenericRepository<AggregateRecord, EntityId> {

    /**
     * Saves the given valid entity, returning a possibly new entity representing the saved state.
     *
     * @param entity The entity to save. Must not be {@literal null}.
     * @return The saved entity will never be {@literal null}.
     * @throws jakarta.validation.ConstraintViolationException if the entity is {@literal null} or invalid.
     */
    @NonNull
    AggregateRecord save(@Valid @NotNull @NonNull AggregateRecord entity);

    /**
     * This method returns last snapshot.
     *
     * @param aggregateId Aggregate id
     * @return Aggregate record with JSON representation of the AggregateRoot.
     */
    @Query("FROM AggregateRecord ag WHERE ag.aggregateId = :aggregateId order by ag.version desc")
    Optional<AggregateRecord> findLastSnapshotFor(@NonNull Long aggregateId);
}
