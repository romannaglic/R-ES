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
package com.axer.component.engine;

import io.micronaut.core.annotation.NonNull;

/**
 * Main Event sourcing logic.
 *
 * @author romannaglic
 * @since 1.0.0
 */
public interface ApplicationService {
    /**
     * The method accepts commands,
     * execute business logic on provided Aggregate, create one or more Events and save Events in the Event Store.
     *
     * @param command Command object
     * @param aggregateRootClass What object has to be restored from the events to be able to execute business logic and create new event(s).
     * @param aggregateRootId if null, new aggregate is wil be created.
     * @return Aggregate Id //TODO return should be not UUID
     */
    AggregateId executeCommand(@NonNull Object command,
                               @NonNull Class<?> aggregateRootClass,
                               Long aggregateRootId
    );

    /**
     * Executes a command and handles the event sourcing logic.
     *
     * @param command            The command object to execute.
     * @param aggregateRootClass The class of the aggregate root object.
     * @return The aggregate ID of the executed command.
     */
    default AggregateId executeCommand(@NonNull Object command, @NonNull Class<?> aggregateRootClass) {
        return executeCommand(command, aggregateRootClass, null);
    }
}
