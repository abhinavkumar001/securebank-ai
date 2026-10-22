package com.securebank.account.dto;

import com.securebank.account.AccountStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateStatusRequest(@NotNull AccountStatus status, @NotBlank @Size(max = 300) String reason) {}
