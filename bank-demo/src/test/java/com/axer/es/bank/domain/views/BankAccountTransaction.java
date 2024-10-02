package com.axer.es.bank.domain.views;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Introspected
public record BankAccountTransaction(
    @NonNull
    BigDecimal money,
    @NonNull
    LocalDateTime dateCreated
)
{}
