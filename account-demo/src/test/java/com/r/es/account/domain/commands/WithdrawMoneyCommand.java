package com.r.es.account.domain.commands;

import java.math.BigDecimal;

public record WithdrawMoneyCommand(BigDecimal amount) {
}
