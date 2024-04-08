package com.r.es.bank.domain.commands;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;

@Introspected
public record AccountSaveWithIdCommand(
    @NonNull
    String name,
    String username

) { }
