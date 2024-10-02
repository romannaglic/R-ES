package com.axer.es.bank.domain.events;

import com.axer.component.anotation.DomainEvent;
import io.micronaut.core.annotation.NonNull;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

@DomainEvent
public record MoneyWithdrawn(@NonNull
                            @Min(0L)
                            BigDecimal money) { }
