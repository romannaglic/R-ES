package com.r.es.bank.domain.events;


import java.math.BigDecimal;

import com.r.component.anotation.DomainEvent;


@DomainEvent
public record MoneyDeposited(
    @Min(0L)
    BigDecimal money
) {}
