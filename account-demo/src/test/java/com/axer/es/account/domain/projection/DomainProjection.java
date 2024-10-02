package com.axer.es.account.domain.projection;

import com.axer.component.anotation.EventProcessor;
import com.axer.component.anotation.ProjectionBuilder;
import com.axer.es.account.domain.entities.Account;
import com.axer.es.account.domain.events.AccountCreated;
import com.axer.es.account.domain.events.MoneyDeposited;
import com.axer.es.account.domain.events.MoneyWithdraw;

@ProjectionBuilder
public class DomainProjection {

    private final AccountRepository accountRepository;

    public DomainProjection(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @EventProcessor
    public void sync(Object event, Long aggregateId, Long version) {
        if (event instanceof AccountCreated) {
            accountRepository.save(
                    new Account(aggregateId, version, ((AccountCreated) event).accountName())
            );
        } else if (event instanceof MoneyDeposited) {
            Account existingAccount = accountRepository.findByAggregateId(aggregateId);
            existingAccount.setCurrentBalance(existingAccount.getCurrentBalance().add(((MoneyDeposited) event).amount()));
            existingAccount.setVersion(version);
            accountRepository.save(existingAccount);
        } else if (event instanceof MoneyWithdraw) {
            Account existingAccount = accountRepository.findByAggregateId(aggregateId);
            existingAccount.setCurrentBalance(existingAccount.getCurrentBalance().subtract(((MoneyWithdraw) event).amount()));
            existingAccount.setVersion(version);
            accountRepository.save(existingAccount);
        }

    }


}
