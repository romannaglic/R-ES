package com.axer.es.bank.domain.views;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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
