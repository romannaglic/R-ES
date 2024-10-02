package com.axer.es.account.domain.events;

import com.axer.component.anotation.DomainEvent;
import io.micronaut.core.annotation.Introspected;

@DomainEvent
@Introspected
public record AccountCreated(String accountName) {
}
