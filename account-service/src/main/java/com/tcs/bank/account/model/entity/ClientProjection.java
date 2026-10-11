package com.tcs.bank.account.model.entity;

import com.tcs.bank.account.model.enumeration.ClientStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "client_projection")
@Getter
@Setter
@NoArgsConstructor
public class ClientProjection {

    @Id
    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "client_name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_status", nullable = false, length = 10)
    private ClientStatus status;

    @Column(name = "last_event_id", nullable = false, length = 36)
    private String lastEventId;

    @Column(name = "last_event_at", nullable = false)
    private Instant lastEventAt;
}
