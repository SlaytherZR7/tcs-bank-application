package com.tcs.bank.client.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcs.bank.client.exceptions.DuplicateIdentificationException;
import com.tcs.bank.client.model.dto.ClientRequest;
import com.tcs.bank.client.model.dto.ClientResponse;
import com.tcs.bank.client.model.enumeration.Gender;
import com.tcs.bank.client.service.ClientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(ClientController.class)
@Import(ClientExceptionHandler.class)
class ClientControllerTest {

    private static final String REQUEST_JSON = """
            {
              "nombre": "Ana Pérez",
              "genero": "Femenino",
              "edad": 32,
              "identificacion": "ID-100",
              "direccion": "Quito",
              "telefono": "0990000000",
              "contrasena": "1234",
              "estado": true
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientService clientService;

    @Test
    @DisplayName("POST clientes creates the resource and omits the password from the response")
    void create_shouldReturnCreatedClientWithoutPassword_whenPayloadIsValid() throws Exception {
        // Arrange
        when(clientService.create(any(ClientRequest.class)))
                .thenReturn(
                        new ClientResponse(
                                1L, "Ana Pérez", Gender.FEMALE, 32, "ID-100", "Quito", "0990000000", true));

        // Act
        ResultActions result = mockMvc.perform(
                post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content(REQUEST_JSON));

        // Assert
        result
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/clientes/1"))
                .andExpect(jsonPath("$.clienteId").value(1))
                .andExpect(jsonPath("$.identificacion").value("ID-100"))
                .andExpect(jsonPath("$.genero").value("Femenino"))
                .andExpect(jsonPath("$.contrasena").doesNotExist());
        verify(clientService).create(any(ClientRequest.class));
    }

    @Test
    @DisplayName("POST clientes returns conflict when the identification already exists")
    void create_shouldReturnConflict_whenIdentificationExists() throws Exception {
        // Arrange
        when(clientService.create(any(ClientRequest.class)))
                .thenThrow(new DuplicateIdentificationException());

        // Act
        ResultActions result = mockMvc.perform(
                post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content(REQUEST_JSON));

        // Assert
        result
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Ya existe un cliente con esa identificación"));
    }

    @Test
    @DisplayName("POST clientes returns a validation error for invalid input")
    void create_shouldReturnBadRequest_whenPayloadIsInvalid() throws Exception {
        // Arrange
        String invalidRequest = REQUEST_JSON.replace("\"edad\": 32", "\"edad\": 0");

        // Act
        ResultActions result = mockMvc.perform(
                post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content(invalidRequest));

        // Assert
        result
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("edad: debe ser mayor que 0"));
    }
}
