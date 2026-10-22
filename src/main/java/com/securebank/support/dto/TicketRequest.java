package com.securebank.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TicketRequest(@NotBlank @Size(min = 10, max = 1000) String message) {}
