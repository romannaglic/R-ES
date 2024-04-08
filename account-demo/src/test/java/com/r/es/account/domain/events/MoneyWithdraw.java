package com.r.es.account.domain.events;

import com.r.component.anotation.DomainEvent;
import java.math.BigDecimal;

@DomainEvent("TakeMoneyFromAccount")
public record MoneyWithdraw(BigDecimal amount) { }
