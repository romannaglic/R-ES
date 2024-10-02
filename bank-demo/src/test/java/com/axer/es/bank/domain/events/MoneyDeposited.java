package com.axer.es.bank.domain.events;


import java.math.BigDecimal;

import com.axer.component.anotation.DomainEvent;
import jakarta.validation.constraints.Min;


@DomainEvent
public record MoneyDeposited(
    @Min(0L)
    BigDecimal money
) {}
