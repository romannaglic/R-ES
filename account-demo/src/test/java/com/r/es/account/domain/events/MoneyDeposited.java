package com.r.es.account.domain.events;

import com.r.component.anotation.DomainEvent;
import java.math.BigDecimal;

@DomainEvent
public record MoneyDeposited(BigDecimal amount) { }
