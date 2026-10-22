package com.securebank.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolveRequest(@NotBlank @Size(max = 500) String resolution) {}
