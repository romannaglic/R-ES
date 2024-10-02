package com.axer.es.bank.domain.events;

import com.axer.component.anotation.DomainEvent;
import io.micronaut.core.annotation.NonNull;

@DomainEvent
public record AccountCreated(
    @NonNull String accountNumber,
    @NonNull String name) {
}
