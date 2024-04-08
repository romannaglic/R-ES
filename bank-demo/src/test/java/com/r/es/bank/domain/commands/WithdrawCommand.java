package com.r.es.bank.domain.commands;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;
import java.math.BigDecimal;
@Introspected
public record WithdrawCommand(
    @Min(value = 0L)
    @NonNull
    BigDecimal money,
    @NonNull
    String accountNumber
) { }
