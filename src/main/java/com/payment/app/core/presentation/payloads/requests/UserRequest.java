package com.payment.app.core.presentation.payloads.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequest(
    @Email
    @NotBlank
    String email,

    @NotBlank
    @Size(max = 50, message = "Username deve ter no máximo 50 caracteres")
    String username,

    @NotBlank
    @Size(min = 8, max = 100, message = "Senha deve ter entre 8 e 100 caracteres")
    String password
) {}
