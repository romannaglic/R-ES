package com.r.es.bank.domain.views;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;

@Introspected
public record BankUser(
    @NonNull
    @NotBlank
    String username,
    @NonNull
    @NotNull
    Boolean owner
) {
  }
