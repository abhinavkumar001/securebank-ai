package com.securebank.account.dto;

import com.securebank.account.AccountType;
import jakarta.validation.constraints.NotNull;

public record OpenAccountRequest(@NotNull AccountType type) {}
