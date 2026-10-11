package com.tcs.bank.client.model.entity;

import com.tcs.bank.client.model.enumeration.ClientStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "clients", uniqueConstraints = @UniqueConstraint(name = "uk_clients_identification", columnNames = "identification"))
@Getter
@Setter
@NoArgsConstructor
public class Client extends Person {

    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private ClientStatus status = ClientStatus.ACTIVE;

    public void markDeleted() {
        status = ClientStatus.DELETED;
    }
}
