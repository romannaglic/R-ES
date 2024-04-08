package account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.tsid.TsidCreator;
import com.r.component.engine.AggregateId;
import com.r.component.engine.ApplicationService;
import com.r.component.exceptions.EventStoreException;
import com.r.data.jpa.entities.AggregateRecord;
import com.r.data.jpa.entities.EntityId;
import com.r.data.jpa.entities.EventRecord;
import com.r.es.account.domain.aggregate.BankAccountAggregate;
import com.r.es.account.domain.commands.CreateAccountCommand;
import com.r.es.account.domain.commands.DepositMoneyCommand;
import com.r.es.account.domain.commands.WithdrawMoneyCommand;
import com.r.es.account.domain.entities.Account;
import com.r.es.account.domain.events.AccountCreated;
import com.r.es.account.domain.exeptions.InsufficientAmountException;
import com.r.es.account.domain.exeptions.InvalidAccountNameException;
import com.r.es.account.domain.projection.AccountRepository;
import com.r.es.account.domain.repository.AggregateRecordCrudRepository;
import com.r.es.account.domain.repository.EventRecordCrudRepository;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This class contains the unit tests for the DefaultApplicationService class.
 */
@MicronautTest(transactional = false)
class DefaultApplicationServiceTest {

    @Inject
    ObjectMapper om;
    @Inject
    ApplicationService applicationService;

    @Inject
    AggregateRecordCrudRepository snapshotsRepository; // used just for testing

    @Inject
    AccountRepository accountRepository;

    @Inject
    EventRecordCrudRepository eventRecordCrudRepository; // used just for testing

    final Long aggRootId = TsidCreator.getTsid().toLong();

    @BeforeEach
    void setUp() {
        eventRecordCrudRepository.deleteAll();
        snapshotsRepository.deleteAll();
        accountRepository.deleteAll();
        eventRecordCrudRepository.save(new EventRecord(aggRootId, 0L, "{ \"accountNumber\":  \"xyz-1\"}", "AccountCreated"));
        eventRecordCrudRepository.save(new EventRecord(aggRootId, 1L, "{ \"accountNumber\":  \"xyz-2\"}", "AccountCreated"));
        eventRecordCrudRepository.save(new EventRecord(aggRootId, 2L, "{ \"accountNumber\":  \"xyz-3\"}", "AccountCreated"));
    }

    @Test
    void executeCommandNewAggregateRoot() {
        AggregateId aggId = null;
        try {
            aggId = applicationService.executeCommand(new CreateAccountCommand("account_1"), BankAccountAggregate.class, null);
        } catch (Throwable e) {
            fail();
        }
        assertNotNull(aggId);
        Optional<EventRecord> eventRecord = eventRecordCrudRepository.findById(new EntityId(aggId.getId(), 0));
        if (eventRecord.isEmpty()) fail();
        assertEquals(aggId.getId(), eventRecord.get().getAggregateId());
        assertEquals(1, accountRepository.findAll().spliterator().estimateSize());
    }

    @Test
    void executeCommandExistingAggregateRoot() {
        AggregateId aggId = null;
        try {
            aggId = applicationService.executeCommand(new CreateAccountCommand("account_1"), BankAccountAggregate.class, aggRootId);
        } catch (Throwable e) {
            fail();
        }
        assertNotNull(aggId);
        assertEquals(aggId.getVersion(), 3);
        assertEquals(4, eventRecordCrudRepository.findAll().spliterator().estimateSize());
        Optional<EventRecord> eventRecord = eventRecordCrudRepository.findById(new EntityId(aggId.getId(), 3));
        if (eventRecord.isEmpty()) fail();
        assertEquals(aggId.getId(), eventRecord.get().getAggregateId());
        assertEquals(1, accountRepository.findAll().spliterator().estimateSize());
    }

    @Test
    void executeCommandThrowException() {
        EventStoreException e = Assertions.assertThrows(
                EventStoreException.class,
                () -> applicationService.executeCommand(new CreateAccountCommand("test"), BankAccountAggregate.class, aggRootId)
        );
        assertEquals(e.getCause().getClass(), InvalidAccountNameException.class);
        assertEquals(3, eventRecordCrudRepository.findAll().spliterator().estimateSize());

    }

    @Test
    void executeExistingSnapshot() throws JsonProcessingException {
        AggregateId rootId = null;
        try {
            rootId = applicationService.executeCommand(new CreateAccountCommand("account_1"), BankAccountAggregate.class, null);
            applicationService.executeCommand(new CreateAccountCommand("account_2"), BankAccountAggregate.class, rootId.getId());
            applicationService.executeCommand(new CreateAccountCommand("account_3"), BankAccountAggregate.class, rootId.getId());
            applicationService.executeCommand(new CreateAccountCommand("account_4"), BankAccountAggregate.class, rootId.getId());
            applicationService.executeCommand(new CreateAccountCommand("account_5"), BankAccountAggregate.class, rootId.getId());
            applicationService.executeCommand(new CreateAccountCommand("account_6"), BankAccountAggregate.class, rootId.getId());
            applicationService.executeCommand(new CreateAccountCommand("account_7"), BankAccountAggregate.class, rootId.getId());
            applicationService.executeCommand(new CreateAccountCommand("account_8"), BankAccountAggregate.class, rootId.getId());
            applicationService.executeCommand(new CreateAccountCommand("account_9"), BankAccountAggregate.class, rootId.getId());
            applicationService.executeCommand(new CreateAccountCommand("account_10"), BankAccountAggregate.class, rootId.getId());
            applicationService.executeCommand(new CreateAccountCommand("account_11"), BankAccountAggregate.class, rootId.getId());
            applicationService.executeCommand(new CreateAccountCommand("account_12"), BankAccountAggregate.class, rootId.getId());
            applicationService.executeCommand(new CreateAccountCommand("account_13"), BankAccountAggregate.class, rootId.getId());
            applicationService.executeCommand(new CreateAccountCommand("account_14"), BankAccountAggregate.class, rootId.getId());
            applicationService.executeCommand(new CreateAccountCommand("account_15"), BankAccountAggregate.class, rootId.getId());
            applicationService.executeCommand(new CreateAccountCommand("account_16"), BankAccountAggregate.class, rootId.getId());
            applicationService.executeCommand(new CreateAccountCommand("account_17"), BankAccountAggregate.class, rootId.getId());
            applicationService.executeCommand(new CreateAccountCommand("account_18"), BankAccountAggregate.class, rootId.getId());
            applicationService.executeCommand(new CreateAccountCommand("account_19"), BankAccountAggregate.class, rootId.getId());
            applicationService.executeCommand(new CreateAccountCommand("account_20"), BankAccountAggregate.class, rootId.getId());
            applicationService.executeCommand(new CreateAccountCommand("account_21"), BankAccountAggregate.class, rootId.getId());
            rootId = applicationService.executeCommand(new CreateAccountCommand("account_22"), BankAccountAggregate.class, rootId.getId());
        } catch (Throwable e) {
            fail();
        }
        List<EventRecord> eventRecords = eventRecordCrudRepository.findAll().stream().toList();
        assertEquals(25, eventRecords.size());
        assertEquals(rootId.getVersion(), 21);
        List<AggregateRecord> snapshots = snapshotsRepository.findAll().stream().toList();
        assertEquals(4, snapshots.size());
        AggregateRecord snapshot1 = snapshots.get(0);
        Map bankAccount1 = om.readValue(snapshot1.getJson(), Map.class);
        assertEquals("account_5", bankAccount1.get("accountName"));
        AggregateRecord snapshot2 = snapshots.get(3);
        Map bankAccount2 = om.readValue(snapshot2.getJson(), Map.class);
        assertEquals("account_20", bankAccount2.get("accountName"));
        EventRecord lastEventRecord = eventRecords.get(eventRecords.size() - 1);
        AccountCreated accountCreated = om.readValue(lastEventRecord.getJson(), AccountCreated.class);
        assertEquals("account_22", accountCreated.accountName());
    }

    @Test
    void executeCommandLock() {
        SlowTask task = new SlowTask(applicationService, aggRootId);
        ExecutorService threadExecutor = Executors.newSingleThreadExecutor();
        try {
            // Start task on another thread
            Future<AggregateId> futureResult = threadExecutor.submit(task);
            applicationService.executeCommand(new CreateAccountCommand("test1"), BankAccountAggregate.class, aggRootId);
            futureResult.get();
            fail();
        } catch (ExecutionException e) {
            assertTrue(e.getCause().getMessage().contains("com.r.component.exceptions.EventStoreException: org.hibernate.exception.ConstraintViolationException"));
        } catch (InterruptedException e) {
            fail();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }


    private record SlowTask(ApplicationService applicationService, Long aggregateId) implements
        Callable<AggregateId> {

        @Override
            public AggregateId call() {
                try {
                    return applicationService.executeCommand(new CreateAccountCommand("lock"),
                        BankAccountAggregate.class, aggregateId);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        }

    @Test
    void executeFlow() {
        AggregateId aggId = null;
        try {
            aggId = applicationService.executeCommand(new CreateAccountCommand("account_1"), BankAccountAggregate.class, null);
            aggId = applicationService.executeCommand(new DepositMoneyCommand(BigDecimal.TEN), BankAccountAggregate.class, aggId.getId());
            aggId = applicationService.executeCommand(new WithdrawMoneyCommand(BigDecimal.valueOf(100)), BankAccountAggregate.class, aggId.getId());
        } catch (Throwable e) {
            if (e.getCause() == null || !(e.getCause() instanceof InsufficientAmountException)) {
                fail();
            }
            try {
                aggId = applicationService.executeCommand(new WithdrawMoneyCommand(BigDecimal.valueOf(5)), BankAccountAggregate.class, aggId.getId());
                aggId = applicationService.executeCommand(new WithdrawMoneyCommand(BigDecimal.valueOf(2)), BankAccountAggregate.class, aggId.getId());
            } catch (Throwable ex) {
                fail();
            }
        }
        Account account = accountRepository.findByAggregateId(aggId.getId());
        assertEquals(0, account.getCurrentBalance().compareTo(BigDecimal.valueOf(3)));
        AggregateId finalAggId = aggId;
        List<EventRecord> events = eventRecordCrudRepository.findAll().stream().toList()
            .stream().filter(er-> Objects.equals(er.getAggregateId(), finalAggId.getId())).toList();
        assertEquals(4, events.size());
        assertEquals(aggId.getVersion(), 3);
    }
}
