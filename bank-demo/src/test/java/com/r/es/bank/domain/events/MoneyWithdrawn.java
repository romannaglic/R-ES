package com.r.es.bank.domain.events;

import com.r.component.anotation.DomainEvent;
import io.micronaut.core.annotation.NonNull;

import java.math.BigDecimal;

@DomainEvent
public record MoneyWithdrawn(@NonNull
                            @Min(0L)
                            BigDecimal money) { }
