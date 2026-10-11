package com.tcs.bank.account.mappers;

import com.tcs.bank.account.model.dto.AccountResponse;
import com.tcs.bank.account.model.dto.CreateAccountRequest;
import com.tcs.bank.account.model.dto.PatchAccountRequest;
import com.tcs.bank.account.model.dto.UpdateAccountRequest;
import com.tcs.bank.account.model.entity.Account;
import com.tcs.bank.account.model.enumeration.AccountStatus;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "client", ignore = true)
    @Mapping(target = "currentBalance", ignore = true)
    Account toEntity(CreateAccountRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "client", ignore = true)
    @Mapping(target = "currentBalance", ignore = true)
    @Mapping(target = "initialBalance", ignore = true)
    void replace(@MappingTarget Account account, UpdateAccountRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "client", ignore = true)
    @Mapping(target = "currentBalance", ignore = true)
    @Mapping(target = "initialBalance", ignore = true)
    void patch(PatchAccountRequest request, @MappingTarget Account account);

    @Mapping(target = "accountId", source = "id")
    @Mapping(target = "clientId", source = "client.clientId")
    @Mapping(target = "clientName", source = "client.name")
    @Mapping(target = "availableBalance", source = "currentBalance")
    AccountResponse toResponse(Account account);

    default AccountStatus toAccountStatus(Boolean active) {
        return Boolean.TRUE.equals(active) ? AccountStatus.ACTIVE : AccountStatus.INACTIVE;
    }

    default Boolean toBooleanStatus(AccountStatus status) {
        return status == AccountStatus.ACTIVE;
    }

    @AfterMapping
    default void trimAccountNumber(@MappingTarget Account account) {
        if (account.getAccountNumber() != null) {
            account.setAccountNumber(account.getAccountNumber().trim());
        }
    }
}
