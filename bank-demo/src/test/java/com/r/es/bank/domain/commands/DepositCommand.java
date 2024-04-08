package com.r.es.bank.domain.commands;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;

import java.math.BigDecimal;

@Introspected
public record DepositCommand(
    @NonNull
    @Min(value = 0L)
    BigDecimal money,
    @NonNull
    String accountNumber
) {}
