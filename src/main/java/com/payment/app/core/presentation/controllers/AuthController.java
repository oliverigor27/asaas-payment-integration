package com.payment.app.core.presentation.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.payment.app.core.application.exceptions.InvalidCredentialsException;
import com.payment.app.core.application.exceptions.TooManyAttemptsException;
import com.payment.app.core.application.services.LoginService;
import com.payment.app.core.infrastructure.security.JwtUtil;
import com.payment.app.core.presentation.payloads.requests.LoginRequest;
import com.payment.app.core.presentation.payloads.responses.LoginResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {
    private final LoginService loginService;
    private final JwtUtil jwtUtil;

    public AuthController(LoginService service, JwtUtil jwtUtil) {
        this.loginService = service;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request, HttpServletRequest httpRequest) {
        try {
            String token = loginService.authenticateUser(request.email(), request.password(), httpRequest.getRemoteAddr());
            return ResponseEntity.ok(new LoginResponse("Successful Login!", token, "Bearer", jwtUtil.getExpirationSeconds()));
        } catch (InvalidCredentialsException ex) {
            return ResponseEntity.status(401).body(new LoginResponse(ex.getMessage(), null, null, null));
        } catch (TooManyAttemptsException ex) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAgterSeconds()))
                .body(new LoginResponse(ex.getMessage(), null, null, null));
        }
    }
}
