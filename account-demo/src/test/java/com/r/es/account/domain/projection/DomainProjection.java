package com.r.es.account.domain.projection;

import com.r.component.anotation.EventProcessor;
import com.r.component.anotation.ProjectionBuilder;
import com.r.es.account.domain.entities.Account;
import com.r.es.account.domain.events.AccountCreated;
import com.r.es.account.domain.events.MoneyDeposited;
import com.r.es.account.domain.events.MoneyWithdraw;

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
