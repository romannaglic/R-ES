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
package com.r.data.jpa.entities;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.annotation.DateCreated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import java.time.LocalDateTime;

/**
 * The class contains information about serialized event.
 *
 * @author Roman Naglic
 * @since 1.0.0
 */
@Entity
@IdClass(EntityId.class)
public final class EventRecord {
    @Id
    private Long aggregateId;
    @Id
    private Long version;
    @DateCreated
    private LocalDateTime dateCreated;
    @NonNull
    @Column(name = "json", columnDefinition="text")
    private String json;
    @NonNull
    @Column(name = "event_name", columnDefinition="text")
    private String eventName;

    public EventRecord() {
    }

    public EventRecord(Long aggregateId, Long version, @NonNull String json, @NonNull String eventName) {
        this.aggregateId = aggregateId;
        this.version = version;
        this.json = json;
        this.eventName = eventName;
    }

    public Long getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Long aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    @NonNull
    public String getJson() {
        return json;
    }

    public void setJson(@NonNull String json) {
        this.json = json;
    }

    @NonNull
    public String getEventName() {
        return eventName;
    }

    public void setEventName(@NonNull String className) {
        this.eventName = className;
    }
}
