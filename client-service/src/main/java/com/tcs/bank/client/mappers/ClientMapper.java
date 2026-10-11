package com.tcs.bank.client.mappers;

import com.tcs.bank.client.model.dto.ClientRequest;
import com.tcs.bank.client.model.dto.ClientResponse;
import com.tcs.bank.client.model.dto.PatchClientRequest;
import com.tcs.bank.client.model.entity.Client;
import com.tcs.bank.client.model.enumeration.ClientStatus;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    Client toEntity(ClientRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    void replace(@MappingTarget Client client, ClientRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    void patch(PatchClientRequest request, @MappingTarget Client client);

    @Mapping(target = "clientId", source = "id")
    ClientResponse toResponse(Client client);

    default ClientStatus toClientStatus(Boolean active) {
        return Boolean.TRUE.equals(active) ? ClientStatus.ACTIVE : ClientStatus.INACTIVE;
    }

    default Boolean toBooleanStatus(ClientStatus status) {
        return status == ClientStatus.ACTIVE;
    }

    @AfterMapping
    default void trimStringFields(@MappingTarget Client client) {
        if (client.getName() != null) {
            client.setName(client.getName().trim());
        }
        if (client.getIdentification() != null) {
            client.setIdentification(client.getIdentification().trim());
        }
        if (client.getAddress() != null) {
            client.setAddress(client.getAddress().trim());
        }
        if (client.getPhone() != null) {
            client.setPhone(client.getPhone().trim());
        }
    }
}
