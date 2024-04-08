package com.r.es.bank.domain.aggregates;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.r.component.anotation.AggregateRoot;
import com.r.component.anotation.CommandProcessor;
import com.r.component.anotation.EventHandler;
import com.r.es.bank.domain.commands.AccountSaveWithIdCommand;
import com.r.es.bank.domain.commands.DepositCommand;
import com.r.es.bank.domain.commands.WithdrawCommand;
import com.r.es.bank.domain.events.AccessGranted;
import com.r.es.bank.domain.events.AccountCreated;
import com.r.es.bank.domain.events.MoneyDeposited;
import com.r.es.bank.domain.events.MoneyWithdrawn;
import com.r.es.bank.domain.exceptions.InsufficientFundsException;
import io.micronaut.core.annotation.NonNull;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@JsonInclude()
@AggregateRoot(snapshotAfter = 5)
public class BankAccount {
    @NonNull
    private BigDecimal balance = BigDecimal.ZERO;
    @JsonIgnore
    private final DefaultAccountNumberGenerator accountNumberGenerator;

    public BankAccount(DefaultAccountNumberGenerator accountNumberGenerator) {
        this.accountNumberGenerator = accountNumberGenerator;
    }

    @NonNull
    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(@NonNull BigDecimal balance) {
        this.balance = balance;
    }
    @EventHandler
    public void eventHandler(Object event) {
        if (event instanceof AccountCreated) {
            setBalance(new BigDecimal("0"));
        } else if (event instanceof AccessGranted) {
            // not yet implemented
        } else if (event instanceof MoneyDeposited) {
            balance = balance.add(((MoneyDeposited) event).money());
        } else if (event instanceof MoneyWithdrawn) {
            balance = balance.subtract(((MoneyWithdrawn) event).money());
        }
    }

    @CommandProcessor
    public List<?> commandProcessor(Object command) {
        if (command instanceof AccountSaveWithIdCommand) {
            String generatedAccountNumber = accountNumberGenerator.generate();
            return Arrays.asList(new AccountCreated(generatedAccountNumber,
                ((AccountSaveWithIdCommand) command).name()),
                new AccessGranted(((AccountSaveWithIdCommand) command).username(), true));
        } else if (command instanceof DepositCommand) {
            return Collections.singletonList(new MoneyDeposited(((DepositCommand) command).money()));
        } else if (command instanceof WithdrawCommand) {
            if (!enoughBalance((WithdrawCommand) command)) {
                throw new InsufficientFundsException();
            }
            return Collections.singletonList(new MoneyWithdrawn(((WithdrawCommand) command).money()));
        }
        return Collections.emptyList();
    }

    private boolean enoughBalance(WithdrawCommand command) {
        return getBalance().subtract(command.money()).compareTo(BigDecimal.ZERO) > 0;
    }


}
