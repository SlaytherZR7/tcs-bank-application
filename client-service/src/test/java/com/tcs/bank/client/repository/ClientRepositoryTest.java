package com.tcs.bank.client.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcs.bank.client.model.entity.Client;
import com.tcs.bank.client.model.enumeration.Gender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class ClientRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ClientRepository clientRepository;

    private Client jose;
    private Client marianela;

    @BeforeEach
    void setUp() {
        jose = persistClient("Jose Lema", "1712345678");
        marianela = persistClient("Marianela Montalvo", "1798765432");
        entityManager.flush();
    }

    @Test
    @DisplayName("existsByIdentification finds a registered identification")
    void existsByIdentification_shouldReturnTrue_whenIdentificationIsRegistered() {
        assertThat(clientRepository.existsByIdentification("1712345678")).isTrue();
    }

    @Test
    @DisplayName("existsByIdentification ignores identifications that are not registered")
    void existsByIdentification_shouldReturnFalse_whenIdentificationIsUnknown() {
        assertThat(clientRepository.existsByIdentification("0000000000")).isFalse();
    }

    @Test
    @DisplayName("existsByIdentificationAndIdNot ignores the client being updated")
    void existsByIdentificationAndIdNot_shouldReturnFalse_whenIdentificationBelongsToSameClient() {
        assertThat(clientRepository.existsByIdentificationAndIdNot("1712345678", jose.getId())).isFalse();
    }

    @Test
    @DisplayName("existsByIdentificationAndIdNot detects an identification used by another client")
    void existsByIdentificationAndIdNot_shouldReturnTrue_whenIdentificationBelongsToAnotherClient() {
        assertThat(clientRepository.existsByIdentificationAndIdNot("1712345678", marianela.getId())).isTrue();
    }

    private Client persistClient(String name, String identification) {
        Client client = new Client();
        client.setName(name);
        client.setGender(Gender.FEMALE);
        client.setAge(30);
        client.setIdentification(identification);
        client.setAddress("Amazonas y NNUU");
        client.setPhone("097548965");
        client.setPasswordHash("$2a$10$abcdefghijklmnopqrstuuJ7ypq0Hc3f2QxvG6m1vHq0VfW1yX2Zy");
        return entityManager.persist(client);
    }
}
