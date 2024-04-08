package com.r.es.bank.domain.views;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;

import java.math.BigDecimal;
import java.util.List;

@Introspected
public record AccountDetails(
    @NonNull
    @Min(value = 0)
    BigDecimal balance,
    String accountNumber,
    @NonNull List<BankUser> users,
    @NonNull List<BankAccountTransaction> transactions
) {
}
