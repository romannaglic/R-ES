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


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.axer.component.anotation.AggregateRoot;
import com.axer.component.anotation.CommandProcessor;
import com.axer.component.anotation.DomainEvent;
import com.axer.component.anotation.EventHandler;
import com.axer.component.anotation.EventProcessor;
import com.axer.component.anotation.ProjectionBuilder;
import com.axer.component.engine.AggregateId;
import com.axer.component.engine.ApplicationService;
import com.axer.component.exceptions.EventStoreException;
import com.axer.component.exceptions.MissingEventHandlerAnnotationException;
import com.axer.component.exceptions.MissingSyncHandlerAnnotationException;
import com.axer.data.jpa.entities.AggregateRecord;
import com.axer.data.jpa.entities.EntityId;
import com.axer.data.jpa.entities.EventRecord;
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

    /**
     * Constructor.
     *
     * @param repository          EventRecordRepository
     * @param om                  ObjectMapper
     * @param beanContext         ApplicationContext
     * @param aggregateRecordRepository AggregateRecordRepository
     */
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
     * Initialization method, annotated with {@link PostConstruct}, which retrieves domain event
     * definitions and synchronous projection definitions from the application context.
     * <p>
     * For each domain event definition, the event name is retrieved from the {@link DomainEvent}
     * annotation and stored in a map.
     * <p>
     * For each synchronous projection definition, the projection object is retrieved from the
     * application context and stored in a map. The event processor method is retrieved from the
     * projection bean definition and stored in the map as well.
     */
    @PostConstruct
    protected void init() {
        // Retrieve domain event definitions
        Collection<BeanDefinition<?>> domainEventDefinitions = beanContext.getBeanDefinitions(Qualifiers.byStereotype(DomainEvent.class));
        domainEventDefinitions.forEach(definition -> {
            AnnotationValue<DomainEvent> domainEventAnnotation = definition.getAnnotationMetadata().getAnnotation(DomainEvent.class);
            if (domainEventAnnotation != null) {
                String eventName = domainEventAnnotation.stringValue("value")
                        .orElse(definition.getBeanType().getSimpleName());
                events.put(eventName, definition.getBeanType());
            }
        });

        // Retrieve synchronous projection definitions
        Collection<BeanDefinition<?>> projectionDefinitions = beanContext.getBeanDefinitions(Qualifiers.byStereotype(ProjectionBuilder.class));
        if (projectionDefinitions.size() > 1) {
            throw new RuntimeException("Multiple synchronous projection classes found");
        }

        // Initialize synchronous projection information
        for (BeanDefinition<?> projectionDefinition : projectionDefinitions) {
            Object projectionObject = beanContext.getBean(projectionDefinition);
            BeanDefinition<?> projectionBeanDefinition = beanContext.getBeanDefinition(projectionDefinition.getBeanType());
            Collection<? extends ExecutableMethod<?, ?>> projectionMethods = projectionBeanDefinition.getExecutableMethods();
            ExecutableMethod<Object, Object> eventProcessorMethod = getEventProcessorMethod(projectionMethods);
            syncProjectionInfo = new SyncProjectionInfo(projectionObject, eventProcessorMethod);
        }
    }

    /**
     * Internal method to execute command.
     *
     * @param command            Command object to process
     * @param aggregateRootClass Class of the aggregate root object
     * @param aggregateRootId    ID of the aggregate root, if null create a new one
     * @return Aggregate ID of the processed command
     * @throws JsonProcessingException If there is an error serializing the event to JSON
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
        Long version = eventRecords.getLast().getVersion();
        saveSnapshot(aggregateInfo, stateInfo, version);
        if (syncProjectionInfo != null) {
            syncEvents(syncProjectionInfo, events, stateInfo);
        }
        repository.saveAll(eventRecords);
        return new AggregateId(stateInfo.getEventEntityId().getAggregateId(), version);
    }

    /**
     * Save the snapshot of the aggregate root when the condition is met.
     * The condition is when the number of events since the last snapshot is greater than or equal to the
     * snapshotAfter value.
     *
     * @param aggregateInfo The aggregate root information
     * @param stateInfo      The current state of the aggregate root
     * @param currentVersion The current version of the aggregate root
     * @throws JsonProcessingException If there is an error serializing the aggregate root to JSON
     */
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

    /**
     * Decides if it is time to save the snapshot of the aggregate root.
     * The condition is when the number of events since the last snapshot is greater than or equal to the
     * snapshotAfter value.
     *
     * @param currentVersion The current version of the aggregate root
     * @param snapshotAfter  The number of events since the last snapshot when the snapshot should be saved
     * @return true if it is time to save the snapshot, false otherwise
     */
    private boolean isItTimeForSnapshot(Long currentVersion, Long snapshotAfter) {
        if (snapshotAfter == null || snapshotAfter == 0) {
            return false;
        }
        return currentVersion % snapshotAfter == 0;
    }

    /**
     * Syncs the events to the projection.
     *
     * @param syncProjectionInfo The information about the projection to be synced
     * @param events The list of events to be synced
     * @param stateInfo The current state of the aggregate root
     */
    private void syncEvents(SyncProjectionInfo syncProjectionInfo, List<?> events, StateInfo stateInfo)  {
        Long aggregateId = stateInfo.getEventEntityId().getAggregateId();
        EntityId entityId = stateInfo.getEventEntityId();
        long version = entityId.getVersion();
        for (Object event : events) {
            version++;
            syncProjectionInfo.getSyncMethod().invoke(syncProjectionInfo.getSyncObject(), event, aggregateId, version);
        }
    }

    /**
     * Prepares the list of EventRecord objects from the list of events.
     *
     * @param events   The list of events
     * @param stateInfo The current state of the aggregate root
     * @return The list of EventRecord objects
     * @throws JsonProcessingException If there is an error serializing the event to JSON
     */
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

    /**
     * Gets the name of the event.
     * The name of the event is determined by the value of the DomainEvent annotation of the event class.
     * If the value of the DomainEvent annotation is null or empty, the name of the event is set to the name of the event class.
     *
     * @param event The event for which to get the name.
     * @return The name of the event.
     */
    private String getNameForEvent(Object event) {
        DomainEvent annotation = event.getClass().getDeclaredAnnotation(DomainEvent.class);
        String domainName = annotation.value();
        if (domainName == null || domainName.isEmpty()) {
            domainName = event.getClass().getSimpleName();
        }
        return domainName;
    }

    /**
     * Prepares the aggregate root information.
     *
     * @param aggregateRootClass The class of the aggregate root
     * @return The aggregate root information
     */
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

    /**
     * Gets the method which is annotated with {@link CommandProcessor} from the methods of the given bean.
     *
     * @param executableMethods The methods of the bean
     * @return The method which is annotated with {@link CommandProcessor}, or throws {@link MissingEventHandlerAnnotationException} if none
     */
    private ExecutableMethod<Object, Object> getExecuteCommandMethod(Collection<? extends ExecutableMethod<?, ?>> executableMethods) {
        for (ExecutableMethod<?, ?> executableMethod : executableMethods) {
            if (executableMethod.isAnnotationPresent(CommandProcessor.class)) {
                return (ExecutableMethod<Object, Object>) executableMethod;
            }
        }
        throw new MissingEventHandlerAnnotationException();
    }

    /**
     * Restores the aggregate state by loading the last snapshot and the subsequent events, and then applying the events to the aggregate root.
     *
     * @param aggregateInfo The aggregate root information
     * @param aggregateRootId The ID of the aggregate root, or null if a new aggregate root should be created
     * @return The current state of the aggregate root
     * @throws JsonProcessingException If there is an error deserializing the snapshot and events
     */
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

    /**
     * Deserializes the given event record to an event object.
     *
     * @param eventRecord The event record to deserialize
     * @param events The map of event class names to their respective classes
     * @return The deserialized event object
     * @throws JsonProcessingException If there is an error deserializing the event
     */
    private Object createDomainEvent(EventRecord eventRecord, Map<String, Class<?>> events) throws JsonProcessingException {
        Class<?> eventClazz = events.get(eventRecord.getEventName());
        return om.readValue(eventRecord.getJson(), eventClazz);
    }

    /**
     * Gets the method which is annotated with {@link EventHandler} from the methods of the given bean.
     *
     * @param executableMethods The methods of the bean
     * @return The method which is annotated with {@link EventHandler}, or throws {@link MissingEventHandlerAnnotationException} if none
     */
    private ExecutableMethod<Object, Object> getHandleMethod(Collection<? extends ExecutableMethod<?, ?>> executableMethods) {
        for (ExecutableMethod<?, ?> executableMethod : executableMethods) {
            if (executableMethod.isAnnotationPresent(EventHandler.class)) {
                return (ExecutableMethod<Object, Object>) executableMethod;
            }
        }
        throw new MissingEventHandlerAnnotationException();
    }

    /**
     * Gets the method which is annotated with {@link EventProcessor} from the methods of the given bean.
     *
     * @param executableMethods The methods of the bean
     * @return The method which is annotated with {@link EventProcessor}, or throws {@link MissingEventHandlerAnnotationException} if none
     */
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
