package com.payment.app.core.presentation.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.payment.app.core.application.services.CustomerService;
import com.payment.app.core.presentation.payloads.requests.CustomerRequest;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController 
@RequestMapping("/v1/user/customer")
public class CustomerController {
    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @PostMapping("")
    public ResponseEntity<?> createCustomer(@AuthenticationPrincipal Long userId, @RequestBody @Valid CustomerRequest entity) {
        service.createCustomer(entity, userId);
        return ResponseEntity.ok().build();
    }
}
