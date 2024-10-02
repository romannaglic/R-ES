package com.axer.es.bank.domain.aggregates;

import io.micronaut.core.annotation.NonNull;
import jakarta.inject.Singleton;

import java.util.UUID;

@Singleton
public class DefaultAccountNumberGenerator {
    @NonNull
    public String generate() {
        return UUID.randomUUID().toString();
    }
}
