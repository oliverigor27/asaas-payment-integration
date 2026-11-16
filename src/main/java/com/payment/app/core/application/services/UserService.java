package com.payment.app.core.application.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.payment.app.core.application.mappers.UserMapper;
import com.payment.app.core.infrastructure.models.User;
import com.payment.app.core.infrastructure.repositories.UserRepository;
import com.payment.app.core.presentation.payloads.requests.UserRequest;

@Service 
public class UserService {
    @Autowired
    private UserRepository repository;

    @Autowired
    private UserMapper mapper;

    @Autowired 
    private PasswordEncoder passwordEncoder;
    
    public void create(UserRequest request) {
        User model = mapper.toModel(request);
        String encodedPassword = passwordEncoder.encode(model.getPassword());
        model.setPassword(encodedPassword);
        repository.save(model);
    }
}
