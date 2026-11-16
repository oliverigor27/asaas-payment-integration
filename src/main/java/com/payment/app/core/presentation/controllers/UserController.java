package com.payment.app.core.presentation.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.payment.app.core.application.services.UserService;
import com.payment.app.core.presentation.payloads.requests.UserRequest;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/v1/users")
public class UserController {
    @Autowired 
    private UserService service;

    @PostMapping("")
    public ResponseEntity<?> createUser(@RequestBody @Valid UserRequest request) {
        service.create(request);
        return ResponseEntity.ok().build();
    }
}
