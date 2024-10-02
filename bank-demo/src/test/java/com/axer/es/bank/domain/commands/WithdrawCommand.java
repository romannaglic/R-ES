package com.axer.es.bank.domain.commands;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
@Introspected
public record WithdrawCommand(
    @Min(value = 0L)
    @NonNull
    BigDecimal money,
    @NonNull
    String accountNumber
) { }
