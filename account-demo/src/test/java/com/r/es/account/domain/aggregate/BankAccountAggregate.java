package com.r.es.account.domain.aggregate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.r.component.anotation.AggregateRoot;
import com.r.component.anotation.CommandProcessor;
import com.r.component.anotation.EventHandler;
import com.r.es.account.domain.commands.CreateAccountCommand;
import com.r.es.account.domain.commands.DepositMoneyCommand;
import com.r.es.account.domain.commands.WithdrawMoneyCommand;
import com.r.es.account.domain.events.AccountCreated;
import com.r.es.account.domain.events.MoneyDeposited;
import com.r.es.account.domain.events.MoneyWithdraw;
import com.r.es.account.domain.exeptions.InsufficientAmountException;
import com.r.es.account.domain.exeptions.InvalidAccountNameException;
import com.r.es.account.domain.exeptions.MissingAccountException;
import io.micronaut.core.annotation.Introspected;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Introspected
@AggregateRoot(snapshotAfter = 5)
public class BankAccountAggregate {
    @JsonProperty("accountName") private String accountName;
    @JsonProperty("amountOnAccount") private BigDecimal currentBalance = BigDecimal.ZERO;

    @EventHandler
    public void eventHandler(Object event) {
        if (event instanceof AccountCreated) {
            accountName = ((AccountCreated) event).accountName();
        } else if (event instanceof MoneyDeposited) {
            currentBalance = currentBalance.add(((MoneyDeposited) event).amount());
        } else if (event instanceof MoneyWithdraw) {
            currentBalance = currentBalance.subtract(((MoneyWithdraw) event).amount());
        }
    }

    @CommandProcessor
    public List<?> commandProcessor(Object command) {
        if (command instanceof CreateAccountCommand) {
            CreateAccountCommand cmd = (CreateAccountCommand) command;
            if ("test".equals(cmd.accountName())) throw new InvalidAccountNameException();
            if ("lock".equals(cmd.accountName())) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            return Collections.singletonList(new AccountCreated(cmd.accountName()));
        } else if (command instanceof DepositMoneyCommand) {
            if (accountName == null) {
                throw new MissingAccountException();
            }
            DepositMoneyCommand cmd = (DepositMoneyCommand) command;
            return Collections.singletonList(new MoneyDeposited(cmd.amount()));
        } else if (command instanceof WithdrawMoneyCommand) {
            if (accountName == null) {
                throw new MissingAccountException();
            }
            WithdrawMoneyCommand cmd = (WithdrawMoneyCommand) command;
            if (currentBalance.compareTo(cmd.amount()) < 0) {
                throw new InsufficientAmountException();
            }
            return Collections.singletonList(new MoneyWithdraw(cmd.amount()));
        }
        return null;
    }


}
