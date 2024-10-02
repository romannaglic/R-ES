package com.axer.es.bank.domain.events;

import com.axer.component.anotation.DomainEvent;
import io.micronaut.core.annotation.NonNull;

@DomainEvent
public record AccessGranted(
    @NonNull String username,
    @NonNull Boolean owner
) { }

