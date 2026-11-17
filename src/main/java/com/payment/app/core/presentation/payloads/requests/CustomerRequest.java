package com.payment.app.core.presentation.payloads.requests;

import org.hibernate.validator.constraints.br.CPF;

import com.payment.app.core.infrastructure.enums.GenderEnum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
    @CPF 
    @NotBlank
    @Size(min = 11, max = 11)
    String cpf,

    @NotBlank
    @Size(max = 250)
    String name,

    @NotBlank
    String phoneNumber,

    @NotNull 
    GenderEnum gender
) {}
