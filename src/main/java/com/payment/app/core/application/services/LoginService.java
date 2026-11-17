package com.payment.app.core.application.services;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.payment.app.core.application.exceptions.InvalidCredentialsException;
import com.payment.app.core.infrastructure.models.User;
import com.payment.app.core.infrastructure.repositories.UserRepository;
import com.payment.app.core.infrastructure.security.JwtUtil;

@Service 
public class LoginService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final String dummyHash;
    private final LoginAttemptService loginAttemptService;

    public LoginService(UserRepository repository, PasswordEncoder encoder, JwtUtil util, LoginAttemptService loginAttemptService) {
        this.userRepository = repository;
        this.passwordEncoder = encoder;
        this.jwtUtil = util;
        this.loginAttemptService = loginAttemptService;
        this.dummyHash = encoder.encode("senha-que-nao-existe"); 
    }

    public String authenticateUser(String email, String password, String ip) {
        loginAttemptService.verificarIp(ip);
        loginAttemptService.verificarEmail(email);
        Optional<User> user = userRepository.findByEmail(email);
        String hash = user.map(User::getPassword).orElse(dummyHash);
        boolean senhaConfere = passwordEncoder.matches(password, hash);
        if (user.isEmpty() || !senhaConfere) {
            loginAttemptService.registrarFalha(email);
            throw new InvalidCredentialsException();
        }
        loginAttemptService.registrarSucesso(email);
        return jwtUtil.generateToken(user.get().getId());
    }
}
