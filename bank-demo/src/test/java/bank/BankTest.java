package bank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.axer.component.engine.AggregateId;
import com.axer.component.engine.ApplicationService;
import com.axer.component.exceptions.EventStoreException;
import com.axer.es.bank.domain.aggregates.BankAccount;
import com.axer.es.bank.domain.aggregates.DefaultAccountNumberGenerator;
import com.axer.es.bank.domain.commands.AccountSaveWithIdCommand;
import com.axer.es.bank.domain.commands.DepositCommand;
import com.axer.es.bank.domain.commands.WithdrawCommand;
import com.axer.es.bank.domain.entities.Account;
import com.axer.es.bank.domain.exceptions.InsufficientFundsException;
import com.axer.es.bank.domain.repository.JpaAccountRepository;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@MicronautTest(transactional = false)
class BankTest {

    @Inject
    ObjectMapper om;
    @Inject
    ApplicationService applicationService;
    @Inject
    JpaAccountRepository accountRepository;
    @Inject
    DefaultAccountNumberGenerator accountNumberGenerator;

    @Test
    void saveAccount() {
        when(accountNumberGenerator.generate()).thenReturn("1234567890");
        AggregateId aggregateId = applicationService.executeCommand(new AccountSaveWithIdCommand("John Savings", "john@test.com"), BankAccount.class);
        Optional<Account> account = accountRepository.findByAggregateId(aggregateId.getId());
        assertTrue(account.isPresent());
        assertEquals(0.0, account.get().getBalance().doubleValue());
        assertEquals("John Savings", account.get().getAccountName());
        assertEquals("1234567890", account.get().getAccountNumber());
    }


    @Test
    void withdrawWithoutMoneyIsNotPossible() {
        when(accountNumberGenerator.generate()).thenReturn("1234567890");
        AggregateId aggregateId = applicationService.executeCommand(new AccountSaveWithIdCommand("John Savings", "john@test.com"), BankAccount.class);
        Account account = accountRepository.findByAggregateId(aggregateId.getId()).orElseThrow(()->new RuntimeException("Account not found"));

        accountRepository.findAggregateIdByAccountNumber(account.getAccountNumber());
        EventStoreException thrown = assertThrows(EventStoreException.class, () -> {
            applicationService.executeCommand(new WithdrawCommand(
                    new BigDecimal("100"), account.getAccountNumber()),
                BankAccount.class,
                aggregateId.getId());
        });
        assertTrue(thrown.getCause() instanceof InsufficientFundsException);
    }

    @Test
    void accountCrud() {
        when(accountNumberGenerator.generate()).thenReturn("1234567890");

        // create account
        AggregateId aggregateId = applicationService.executeCommand(
            new AccountSaveWithIdCommand("John Savings", "john@test.com"), BankAccount.class);
        Account account = accountRepository.findByAggregateId(aggregateId.getId())
            .orElseThrow(() -> new RuntimeException("Account not found"));

        // deposit money
        aggregateId = applicationService.executeCommand(
            new DepositCommand(new BigDecimal("100"), account.getAccountNumber()),
            BankAccount.class,
            aggregateId.getId());
        account = accountRepository.findByAggregateId(aggregateId.getId()).orElse(null);
        if (account == null) fail();
        assertEquals(new BigDecimal("100").stripTrailingZeros(), account.getBalance().stripTrailingZeros());
        assertEquals(1, account.getTransactions().size());

        // withdraw money
        aggregateId = applicationService.executeCommand(
            new WithdrawCommand(new BigDecimal("45"), account.getAccountNumber()),
            BankAccount.class,
            aggregateId.getId());
        account = accountRepository.findByAggregateId(aggregateId.getId()).orElse(null);
        if (account == null) fail();
        assertEquals(new BigDecimal("55").stripTrailingZeros(), account.getBalance().stripTrailingZeros());
        assertEquals(2, account.getTransactions().size());
    }


    @MockBean(DefaultAccountNumberGenerator.class)
    DefaultAccountNumberGenerator accountNumberGenerator() {
        return mock(DefaultAccountNumberGenerator.class);
    }


}
