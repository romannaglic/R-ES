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
package com.axer.component.anotation;

import io.micronaut.context.annotation.Prototype;
import io.micronaut.core.annotation.Introspected;
import jakarta.inject.Qualifier;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
/**
 * The annotation marks the class as Aggregate Root.
 * The snapshotAfter attribute specifies the number of events that are persisted before the snapshot of
 * the aggregate root is serialized to the database.
 *
 * @author Roman Naglic
 * @since 1.0.0
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target(TYPE)
@Prototype
@Introspected
public @interface AggregateRoot {
    long snapshotAfter() default 0;
}
