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
package com.axer.data.jpa.entities;

import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.data.annotation.Embeddable;
import java.io.Serializable;

/**
 * The primary key used in events and snapshots.
 *
 * @author Roman Naglic
 * @since 1.0.0
 */
@Embeddable
@ReflectiveAccess
public final class EntityId implements Serializable {
    private Long aggregateId;
    private Long version;

    public EntityId() {
    }

    public EntityId(Long aggregateId, long version) {
        this.aggregateId = aggregateId;
        this.version = version;
    }

    public Long getAggregateId() {
        return aggregateId;
    }

    public Long getVersion() {
        return version;
    }

    public void setAggregateId(Long aggregateId) {
        this.aggregateId = aggregateId;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EntityId that)) {
            return false;
        }

        if (!getAggregateId().equals(that.getAggregateId())) {
            return false;
        }
        return getVersion().equals(that.getVersion());
    }

    @Override
    public int hashCode() {
        int result = getAggregateId().hashCode();
        result = 31 * result + getVersion().hashCode();
        return result;
    }
}
