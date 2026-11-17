package com.payment.app.core.application.services;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payment.app.core.application.exceptions.CpfAlreadyRegisteredException;
import com.payment.app.core.application.exceptions.CustomerAlreadyExistsException;
import com.payment.app.core.application.exceptions.UserNotActiveException;
import com.payment.app.core.infrastructure.models.CustomerModel;
import com.payment.app.core.infrastructure.models.User;
import com.payment.app.core.infrastructure.repositories.CustomerRepository;
import com.payment.app.core.infrastructure.repositories.UserRepository;
import com.payment.app.core.presentation.payloads.requests.CustomerRequest;

@Service 
public class CustomerService {
    private final CustomerRepository repository;
    private final UserRepository userRepository;

    public CustomerService(CustomerRepository customerRepository, UserRepository userRepository) {
        this.repository = customerRepository;
        this.userRepository = userRepository;
    }

    @Transactional 
    public void createCustomer(CustomerRequest request, Long userId) {
        User user = findUser(userId);
        if (repository.existsByUserId(userId))
            throw new CustomerAlreadyExistsException();
        if (repository.existsByCpf(normalizeCpf(request.cpf())))
            throw new CpfAlreadyRegisteredException();
        CustomerModel customer = CustomerModel.builder()
            .user(user)
            .cpf(normalizeCpf(request.cpf()))
            .phoneNumber(normalizePhoneString(request.phoneNumber()))
            .name(request.name())
            .gender(request.gender())
            .build();
        try {
            repository.saveAndFlush(customer);
        } catch (DataIntegrityViolationException e) {
            throw new CustomerAlreadyExistsException();
        }
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
            .filter(User::isActive)
            .orElseThrow(UserNotActiveException::new);
    }

    private String normalizeCpf(String cpf) {
        return cpf.replaceAll("\\D", "");
    }

    private String normalizePhoneString(String phone) {
        return phone.replaceAll("\\D", "");
    }
}
