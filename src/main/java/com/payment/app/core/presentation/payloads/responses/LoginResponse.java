package com.payment.app.core.presentation.payloads.responses;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(
    String message,
    String token,
    String tokenType,
    Long expiresIn
) {}
