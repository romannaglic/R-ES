package com.r.es.account.domain.events;

import com.r.component.anotation.DomainEvent;
import io.micronaut.core.annotation.Introspected;

@DomainEvent
@Introspected
public record AccountCreated(String accountName) {
}
