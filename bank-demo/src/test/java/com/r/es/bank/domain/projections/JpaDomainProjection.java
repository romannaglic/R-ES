package com.r.es.bank.domain.projections;

import com.r.component.anotation.EventProcessor;
import com.r.component.anotation.ProjectionBuilder;
import com.r.es.bank.domain.entities.Account;
import com.r.es.bank.domain.entities.Transaction;
import com.r.es.bank.domain.entities.User;
import com.r.es.bank.domain.events.AccessGranted;
import com.r.es.bank.domain.events.AccountCreated;
import com.r.es.bank.domain.events.MoneyDeposited;
import com.r.es.bank.domain.events.MoneyWithdrawn;
import com.r.es.bank.domain.repository.JpaAccountRepository;
import java.math.BigDecimal;

@ProjectionBuilder
public class JpaDomainProjection {

    private final JpaAccountRepository accountRepository;

    public JpaDomainProjection(JpaAccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @EventProcessor
    public void sync(Object event, Long aggregateId, Long version) {
        if (event instanceof AccountCreated) {
            Account entity = new Account();
            entity.setAggregateId(aggregateId);
            entity.setAccountName(((AccountCreated) event).name());
            entity.setAccountNumber(((AccountCreated) event).accountNumber());
            entity.setBalance(new BigDecimal("0"));
            accountRepository.save(entity);
        } else if (event instanceof AccessGranted) {
            accountRepository.findByAggregateId(aggregateId)
                .ifPresent(accountEntity -> {
                    User userEntity = new User();
                    userEntity.setOwner(((AccessGranted) event).owner());
                    userEntity.setUsername(((AccessGranted) event).username());
                    userEntity.setAccount(accountEntity);
                    accountEntity.getUsers().add(userEntity);
                    accountRepository.save(accountEntity);
                });
        } else if (event instanceof MoneyWithdrawn) {
            accountRepository.findByAggregateId(aggregateId)
                .ifPresent(accountEntity -> {
                    Transaction transactionEntity = new Transaction();
                    transactionEntity.setAmount(((MoneyWithdrawn) event).money());
                    transactionEntity.setAccount(accountEntity);
                    accountEntity.setBalance(accountEntity.getBalance().subtract(((MoneyWithdrawn) event).money()));
                    accountEntity.getTransactions().add(transactionEntity);
                    accountRepository.save(accountEntity);
                });
        } else if (event instanceof MoneyDeposited) {
            accountRepository.findByAggregateId(aggregateId)
                .ifPresent(accountEntity -> {
                    Transaction transactionEntity = new Transaction();
                    transactionEntity.setAmount(((MoneyDeposited) event).money());
                    transactionEntity.setAccount(accountEntity);
                    accountEntity.setBalance(accountEntity.getBalance().add(((MoneyDeposited) event).money()));
                    accountEntity.getTransactions().add(transactionEntity);
                    accountRepository.save(accountEntity);
                });
        }
    }

}
