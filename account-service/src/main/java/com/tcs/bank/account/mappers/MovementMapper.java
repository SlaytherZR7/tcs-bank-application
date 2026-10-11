package com.tcs.bank.account.mappers;

import com.tcs.bank.account.model.dto.MovementResponse;
import com.tcs.bank.account.model.entity.Movement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MovementMapper {

    @Mapping(target = "movementId", source = "id")
    @Mapping(target = "accountNumber", source = "account.accountNumber")
    MovementResponse toResponse(Movement movement);
}
