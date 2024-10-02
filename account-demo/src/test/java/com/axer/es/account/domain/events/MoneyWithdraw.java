package com.axer.es.account.domain.events;

import com.axer.component.anotation.DomainEvent;
import java.math.BigDecimal;

@DomainEvent("TakeMoneyFromAccount")
public record MoneyWithdraw(BigDecimal amount) { }
