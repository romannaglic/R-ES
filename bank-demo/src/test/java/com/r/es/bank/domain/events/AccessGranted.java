package com.r.es.bank.domain.events;

import com.r.component.anotation.DomainEvent;
import io.micronaut.core.annotation.NonNull;

@DomainEvent
public record AccessGranted(
    @NonNull String username,
    @NonNull Boolean owner
) { }

