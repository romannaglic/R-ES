package com.axer.es.account.domain.events;

import com.axer.component.anotation.DomainEvent;
import java.math.BigDecimal;

@DomainEvent
public record MoneyDeposited(BigDecimal amount) { }
