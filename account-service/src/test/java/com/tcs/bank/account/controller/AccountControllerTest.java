package com.tcs.bank.account.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcs.bank.account.exceptions.DuplicateAccountNumberException;
import com.tcs.bank.account.model.dto.AccountResponse;
import com.tcs.bank.account.model.dto.CreateAccountRequest;
import com.tcs.bank.account.model.enumeration.AccountType;
import com.tcs.bank.account.service.AccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(AccountController.class)
@Import(AccountExceptionHandler.class)
class AccountControllerTest {

    private static final String REQUEST_JSON = """
            {
              "numeroCuenta": "500001",
              "clienteId": 15,
              "tipoCuenta": "Ahorro",
              "saldoInicial": 125.50,
              "estado": true
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @Test
    @DisplayName("POST cuentas creates the account and returns Spanish JSON properties")
    void create_shouldReturnCreatedAccount() throws Exception {
        // Arrange
        when(accountService.create(any(CreateAccountRequest.class))).thenReturn(
                new AccountResponse(
                        2L,
                        "500001",
                        15L,
                        "Ana Pérez",
                        AccountType.SAVINGS,
                        new java.math.BigDecimal("125.50"),
                        new java.math.BigDecimal("125.50"),
                        true));

        // Act
        ResultActions result = mockMvc.perform(
                post("/api/cuentas").contentType(MediaType.APPLICATION_JSON).content(REQUEST_JSON));

        // Assert
        result.andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/cuentas/2"))
                .andExpect(jsonPath("$.numeroCuenta").value("500001"))
                .andExpect(jsonPath("$.tipoCuenta").value("Ahorro"))
                .andExpect(jsonPath("$.saldoDisponible").value(125.50));
    }

    @Test
    @DisplayName("POST cuentas returns conflict for a duplicate account number")
    void create_shouldReturnConflictForDuplicateAccountNumber() throws Exception {
        // Arrange
        when(accountService.create(any(CreateAccountRequest.class)))
                .thenThrow(new DuplicateAccountNumberException());

        // Act
        ResultActions result = mockMvc.perform(
                post("/api/cuentas").contentType(MediaType.APPLICATION_JSON).content(REQUEST_JSON));

        // Assert
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Ya existe una cuenta con ese número"));
    }

    @Test
    @DisplayName("POST cuentas rejects a negative opening balance")
    void create_shouldRejectNegativeInitialBalance() throws Exception {
        // Arrange
        String invalidRequest = REQUEST_JSON.replace("125.50", "-1.00");

        // Act
        ResultActions result = mockMvc.perform(
                post("/api/cuentas").contentType(MediaType.APPLICATION_JSON).content(invalidRequest));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("saldoInicial")));
    }

    @Test
    @DisplayName("POST cuentas rejects letters in the account number")
    void create_shouldRejectAlphanumericAccountNumber() throws Exception {
        // Arrange
        String invalidRequest = REQUEST_JSON.replace("500001", "50A001");

        // Act
        ResultActions result = mockMvc.perform(
                post("/api/cuentas").contentType(MediaType.APPLICATION_JSON).content(invalidRequest));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("numeroCuenta")));
    }
}
