package com.securebank.payee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PayeeRequest(@NotBlank @Size(max = 60) String name, @NotBlank @Size(max = 20) String accountNumber) {}
