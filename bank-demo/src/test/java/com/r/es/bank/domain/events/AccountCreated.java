package com.r.es.bank.domain.events;

import com.r.component.anotation.DomainEvent;
import io.micronaut.core.annotation.NonNull;

@DomainEvent
public record AccountCreated(
    @NonNull String accountNumber,
    @NonNull String name) {
}
