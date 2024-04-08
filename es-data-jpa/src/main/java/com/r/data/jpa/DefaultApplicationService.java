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
package com.r.data.jpa;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.r.component.anotation.AggregateRoot;
import com.r.component.anotation.CommandProcessor;
import com.r.component.anotation.DomainEvent;
import com.r.component.anotation.EventHandler;
import com.r.component.anotation.EventProcessor;
import com.r.component.anotation.ProjectionBuilder;
import com.r.component.engine.AggregateId;
import com.r.component.engine.ApplicationService;
import com.r.component.exceptions.EventStoreException;
import com.r.component.exceptions.MissingEventHandlerAnnotationException;
import com.r.component.exceptions.MissingSyncHandlerAnnotationException;
import com.r.data.jpa.entities.AggregateRecord;
import com.r.data.jpa.entities.EntityId;
import com.r.data.jpa.entities.EventRecord;
import io.micronaut.context.ApplicationContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import java.util.*;

/**
 * Default implementation of the Application Service.
 *
 * @author Roman Naglic
 * @since 1.0.0
 */
@Singleton
public class DefaultApplicationService implements ApplicationService {
    final Map<String, Class<?>> events = new HashMap<>();
    private final EventRecordRepository repository;
    private final ObjectMapper om;
    private final ApplicationContext beanContext;
    private final AggregateRecordRepository aggregateRecordRepository;
    private SyncProjectionInfo syncProjectionInfo;

    public DefaultApplicationService(EventRecordRepository repository,
                                     ObjectMapper om,
                                     ApplicationContext beanContext,
                                     AggregateRecordRepository aggregateRecordRepository) {
        this.repository = repository;
        this.om = om;
        this.beanContext = beanContext;
        this.aggregateRecordRepository = aggregateRecordRepository;
    }

    /**
     * This method implements.
     *
     * @param command            Command object.
     * @param aggregateRootClass What object has to be restored from the events to be able to execute business logic and create new event(s).
     * @param aggregateRootId    if null, new aggregate is wil be created.
     * @return Existing or new aggregate id.
     * @throws EventStoreException EventStoreException
     */
    @Override
    public AggregateId executeCommand(@NonNull Object command, @NonNull Class<?> aggregateRootClass, Long aggregateRootId) throws EventStoreException {
        try {
            return executeCommandInternal(command, aggregateRootClass, aggregateRootId);
        } catch (Throwable e) {
            if (e.getMessage() != null && e.getMessage().contains("ConstraintViolationException")) {
                throw new EventStoreException("OptimisticLockingException");
            }
            throw new EventStoreException(e);
        }
    }

    /**
     * This method implements ....
     */
    @PostConstruct
    protected void init() {
        Collection<BeanDefinition<?>> definitions = beanContext.getBeanDefinitions(Qualifiers.byStereotype(
            DomainEvent.class));
        definitions.forEach(
            definition -> {
                AnnotationValue<DomainEvent> annotationMetadata = definition.getAnnotationMetadata().getAnnotation(DomainEvent.class);
                if (annotationMetadata != null) {
                    if (annotationMetadata.stringValue("value").isPresent()) {
                        events.put(annotationMetadata.stringValue("value").get(), definition.getBeanType());
                    } else {
                        events.put(definition.getBeanType().getSimpleName(), definition.getBeanType());
                    }
                }
            }
        );
        // find synchronous projection class
        Collection<BeanDefinition<?>> syncDefinitions = beanContext.getBeanDefinitions(Qualifiers.byStereotype(
            ProjectionBuilder.class));
        if (syncDefinitions.size() > 1) {
            throw new RuntimeException();
        }

        for (BeanDefinition<?> definition : syncDefinitions) {
            Object syncObject = beanContext.getBean(definition);
            BeanDefinition<?> beanDefinition = beanContext.getBeanDefinition(definition.getBeanType());
            Collection<? extends ExecutableMethod<?, ?>> executableMethods = beanDefinition.getExecutableMethods();
            ExecutableMethod<Object, Object> syncMethod = getEventProcessorMethod(executableMethods);
            syncProjectionInfo = new SyncProjectionInfo(syncObject, syncMethod);
        }
    }

    /**
     * The implementation of this method.
     *
     * @param command            Command object.
     * @param aggregateRootClass Aggregate class.
     * @param aggregateRootId    Aggregate root id.
     * @return Existing or new Aggregate id.
     * @throws JsonProcessingException   JsonProcessingException
     */
    @Transactional
    protected AggregateId executeCommandInternal(@NonNull Object command,
                                                 @NonNull Class<?> aggregateRootClass,
                                                 Long aggregateRootId) throws JsonProcessingException {
        AggregateInfo aggregateInfo = prepareAggregateInfo(aggregateRootClass);
        StateInfo stateInfo = restoreAggregateState(aggregateInfo, aggregateRootId);
        List<?> events = (List<?>) aggregateInfo.executeCommandMethod.invoke(aggregateInfo.aggregateRoot, command);
        if (events == null) {
            events = Collections.emptyList();
        }
        List<EventRecord> eventRecords = prepareEventRecords(events, stateInfo);
        Long version = eventRecords.get(eventRecords.size() - 1).getVersion();
        saveSnapshot(aggregateInfo, stateInfo, version);
        if (syncProjectionInfo != null) {
            syncEvents(syncProjectionInfo, events, stateInfo);
        }
        repository.saveAll(eventRecords);
        return new AggregateId(stateInfo.getEventEntityId().getAggregateId(), version);
    }

    private void saveSnapshot(AggregateInfo aggregateInfo,
                              StateInfo stateInfo,
                              Long currentVersion) throws JsonProcessingException {
        if (currentVersion != 0 && isItTimeForSnapshot(currentVersion, aggregateInfo.snapshotAfter)) {
            aggregateRecordRepository.save(
                new AggregateRecord(
                    stateInfo.getEventEntityId().getAggregateId(),
                    stateInfo.getEventEntityId().getVersion(),
                    om.writeValueAsString(stateInfo.getAggregateRoot()))
            );
        }
    }

    private boolean isItTimeForSnapshot(Long currentVersion, Long snapshotAfter) {
        if (snapshotAfter == null || snapshotAfter == 0) {
            return false;
        }
        return currentVersion % snapshotAfter == 0;
    }

    private void syncEvents(SyncProjectionInfo syncProjectionInfo, List<?> events, StateInfo stateInfo)  {
        Long aggregateId = stateInfo.getEventEntityId().getAggregateId();
        EntityId entityId = stateInfo.getEventEntityId();
        long version = entityId.getVersion();
        for (Object event : events) {
            version++;
            syncProjectionInfo.getSyncMethod().invoke(syncProjectionInfo.getSyncObject(), event, aggregateId, version);
        }
    }

    private List<EventRecord> prepareEventRecords(List<?> events, StateInfo stateInfo) throws JsonProcessingException {
        List<EventRecord> records = new ArrayList<>();
        EntityId entityId = stateInfo.getEventEntityId();
        long version = entityId.getVersion();
        for (Object event : events) {
            version++;
            records.add(new EventRecord(entityId.getAggregateId(), version, om.writeValueAsString(event), getNameForEvent(event)));
        }
        return records;
    }

    private String getNameForEvent(Object event) {
        DomainEvent annotation = event.getClass().getDeclaredAnnotation(DomainEvent.class);
        String domainName = annotation.value();
        if (domainName == null || domainName.isEmpty()) {
            domainName = event.getClass().getSimpleName();
        }
        return domainName;
    }

    private AggregateInfo prepareAggregateInfo(Class<?> aggregateRootClass) {
        AggregateRoot rootAnnotation = aggregateRootClass.getDeclaredAnnotation(AggregateRoot.class);
        Long snapshotAfter = null;
        if (rootAnnotation != null) {
            snapshotAfter = rootAnnotation.snapshotAfter();
        }
        Object aggregateRoot = beanContext.getBean(aggregateRootClass);
        BeanDefinition<?> beanDefinition = beanContext.getBeanDefinition(aggregateRootClass);
        Collection<? extends ExecutableMethod<?, ?>> executableMethods = beanDefinition.getExecutableMethods();
        ExecutableMethod<Object, Object> handleMethod = getHandleMethod(executableMethods);
        ExecutableMethod<Object, Object> executeCommandMethod = getExecuteCommandMethod(executableMethods);
        return new AggregateInfo(aggregateRoot, handleMethod, executeCommandMethod, snapshotAfter);
    }

    private ExecutableMethod<Object, Object> getExecuteCommandMethod(Collection<? extends ExecutableMethod<?, ?>> executableMethods) {
        for (ExecutableMethod<?, ?> executableMethod : executableMethods) {
            if (executableMethod.isAnnotationPresent(CommandProcessor.class)) {
                return (ExecutableMethod<Object, Object>) executableMethod;
            }
        }
        throw new MissingEventHandlerAnnotationException();
    }

    private StateInfo restoreAggregateState(AggregateInfo aggregateInfo, final Long aggregateRootId) throws JsonProcessingException {
        Long rootId = aggregateRootId;
        if (rootId == null) {
            rootId = TsidUtil.getTsidFactory().create().toLong();
        }
        Object aggregateRoot;
        long lastVersion = -1;
        if (aggregateRootId != null && aggregateInfo.snapshotAfter != null) {
            Optional<AggregateRecord> aggregateRecords = aggregateRecordRepository.findLastSnapshotFor(rootId);
            if (aggregateRecords.isPresent()) {
                AggregateRecord aggregateRecord = aggregateRecords.get();
                aggregateRoot = om.readValue(aggregateRecord.getJson(), aggregateInfo.getAggregateRoot().getClass());
                lastVersion = aggregateRecord.getVersion();
            } else {
                aggregateRoot = aggregateInfo.aggregateRoot;
            }
        } else {
            aggregateRoot = aggregateInfo.aggregateRoot;
        }
        List<EventRecord> eventRecords = repository.loadEventsFromVersion(rootId, lastVersion);
        for (EventRecord eventRecord : eventRecords) {
            Object event = createDomainEvent(eventRecord, events);
            aggregateInfo.handlerMethod.invoke(aggregateRoot, event);
            lastVersion = eventRecord.getVersion();
        }
        return new StateInfo(new EntityId(rootId, lastVersion), aggregateRoot);
    }

    private Object createDomainEvent(EventRecord eventRecord, Map<String, Class<?>> events) throws JsonProcessingException {
        Class<?> eventClazz = events.get(eventRecord.getEventName());
        return om.readValue(eventRecord.getJson(), eventClazz);
    }

    private ExecutableMethod<Object, Object> getHandleMethod(Collection<? extends ExecutableMethod<?, ?>> executableMethods) {
        for (ExecutableMethod<?, ?> executableMethod : executableMethods) {
            if (executableMethod.isAnnotationPresent(EventHandler.class)) {
                return (ExecutableMethod<Object, Object>) executableMethod;
            }
        }
        throw new MissingEventHandlerAnnotationException();
    }

    private ExecutableMethod<Object, Object> getEventProcessorMethod(Collection<? extends ExecutableMethod<?, ?>> executableMethods) {
        for (ExecutableMethod<?, ?> executableMethod : executableMethods) {
            if (executableMethod.isAnnotationPresent(EventProcessor.class)) {
                return (ExecutableMethod<Object, Object>) executableMethod;
            }
        }
        throw new MissingSyncHandlerAnnotationException();
    }

    private static class AggregateInfo {
        private final Object aggregateRoot;
        private final ExecutableMethod<Object, Object> handlerMethod;
        private final ExecutableMethod<Object, Object> executeCommandMethod;
        private final Long snapshotAfter;

        public AggregateInfo(Object aggregateRoot,
                             ExecutableMethod<Object, Object> handlerMethod,
                             ExecutableMethod<Object, Object> executeCommandMethod,
                             Long snapshotAfter) {
            this.aggregateRoot = aggregateRoot;
            this.handlerMethod = handlerMethod;
            this.executeCommandMethod = executeCommandMethod;
            this.snapshotAfter = snapshotAfter;
        }

        public Object getAggregateRoot() {
            return aggregateRoot;
        }
    }

    private static class SyncProjectionInfo {
        private final Object syncObject;
        private final ExecutableMethod<Object, Object> syncMethod;

        public SyncProjectionInfo(Object syncObject,
                                  ExecutableMethod<Object, Object> syncMethod) {
            this.syncObject = syncObject;
            this.syncMethod = syncMethod;
        }

        public Object getSyncObject() {
            return syncObject;
        }

        public ExecutableMethod<Object, Object> getSyncMethod() {
            return syncMethod;
        }
    }

    private static class StateInfo {
        private final EntityId entityId;
        private final Object aggregateRoot;

        public StateInfo(EntityId entityId, Object aggregateRoot) {
            this.entityId = entityId;
            this.aggregateRoot = aggregateRoot;
        }

        public EntityId getEventEntityId() {
            return entityId;
        }

        public Object getAggregateRoot() {
            return aggregateRoot;
        }
    }
}
