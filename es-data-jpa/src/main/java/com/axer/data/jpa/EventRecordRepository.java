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

import com.axer.data.jpa.entities.EntityId;
import com.axer.data.jpa.entities.EventRecord;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.GenericRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * EventStoreRepository.
 */
@Repository
public interface EventRecordRepository extends GenericRepository<EventRecord, EntityId> {

  /**
   * Returns all events of the given aggregate with a version higher than the given
   * version.
   *
   * @param aggregateId The id of the aggregate.
   * @param version      The version of the aggregate.
   * @return A list with all events of the given aggregate with a version higher
   * than the given version.
   */
  @Query("SELECT er FROM EventRecord er WHERE er.aggregateId = :aggregateId and er.version > :version")
  List<EventRecord> loadEventsFromVersion(Long aggregateId, long version);

  /**
   * Saves all given entities, possibly returning new instances representing the saved state.
   *
   * @param entities The entities to save. Must not be {@literal null}.
   * @return The saved entities objects. will never be {@literal null}.
   * @throws org.hibernate.exception.ConstraintViolationException if the entities are {@literal null}.
   */
  @NonNull
  Iterable<EventRecord> saveAll(@Valid @NotNull @NonNull Iterable<EventRecord> entities);
}
