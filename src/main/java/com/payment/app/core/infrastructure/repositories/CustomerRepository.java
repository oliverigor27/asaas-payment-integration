package com.payment.app.core.infrastructure.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.payment.app.core.infrastructure.models.CustomerModel;

public interface CustomerRepository extends JpaRepository<CustomerModel, Long> {
    boolean existsByUserId(Long userId);
    boolean existsByCpf(String cpf);
}
