package com.payment.app.application.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.payment.app.infrastructure.models.User;
import com.payment.app.presentation.payloads.requests.UserRequest;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    @Mapping(target = ".", source = ".")
    User toModel(UserRequest request);
}

