package com.tcs.bank.account.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcs.bank.account.exceptions.InsufficientFundsException;
import com.tcs.bank.account.exceptions.MovementAlreadyCorrectedException;
import com.tcs.bank.account.exceptions.MovementNotFoundException;
import com.tcs.bank.account.model.dto.CreateMovementRequest;
import com.tcs.bank.account.model.dto.MovementResponse;
import com.tcs.bank.account.model.dto.UpdateMovementRequest;
import com.tcs.bank.account.model.enumeration.MovementType;
import com.tcs.bank.account.service.MovementService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(MovementController.class)
@Import(AccountExceptionHandler.class)
class MovementControllerTest {

        private static final String MOVEMENTS_URL = "/api/movimientos";

        private static final LocalDateTime MOVEMENT_DATE = LocalDateTime.of(2022, 2, 10, 10, 0);

        private static final String DEPOSIT_JSON = """
                        {
                          "numeroCuenta": "225487",
                          "valor": 600.00
                        }
                        """;

        private static final String WITHDRAWAL_JSON = """
                        {
                          "numeroCuenta": "496825",
                          "valor": -541.00
                        }
                        """;

        private static final String UPDATE_JSON = """
                        {
                          "valor": -300.00
                        }
                        """;

        private static final String ZERO_AMOUNT_JSON = """
                        {
                          "numeroCuenta": "225487",
                          "valor": 0.00
                        }
                        """;

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private MovementService movementService;

        @Test
        @DisplayName("GET /api/movimientos returns 200 with movement list")
        void findAll_shouldReturn200() throws Exception {
                MovementResponse res1 = new MovementResponse(
                                1L, MOVEMENT_DATE, MovementType.INCOME, new BigDecimal("600.00"),
                                new BigDecimal("700.00"), "225487");
                when(movementService.findAll(null)).thenReturn(List.of(res1));

                mockMvc.perform(get(MOVEMENTS_URL))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1))
                                .andExpect(jsonPath("$[0].movimientoId").value(1))
                                .andExpect(jsonPath("$[0].tipoMovimiento").value("Ingreso"));
        }

        @Test
        @DisplayName("GET /api/movimientos/{id} returns 200 when movement exists")
        void findById_shouldReturn200() throws Exception {
                MovementResponse res = new MovementResponse(
                                1L, MOVEMENT_DATE, MovementType.INCOME, new BigDecimal("600.00"),
                                new BigDecimal("700.00"), "225487");
                when(movementService.findById(1L)).thenReturn(res);

                mockMvc.perform(get(MOVEMENTS_URL + "/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.movimientoId").value(1))
                                .andExpect(jsonPath("$.numeroCuenta").value("225487"));
        }

        @Test
        @DisplayName("GET /api/movimientos/{id} returns 404 when movement does not exist")
        void findById_shouldReturn404_whenNotFound() throws Exception {
                when(movementService.findById(99L)).thenThrow(new MovementNotFoundException(99L));

                mockMvc.perform(get(MOVEMENTS_URL + "/99"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("POST /api/movimientos returns 201 with movement details on successful deposit")
        void create_shouldReturn201_whenDepositSucceeds() throws Exception {
                MovementResponse response = new MovementResponse(
                                1L,
                                LocalDateTime.of(2022, 2, 10, 9, 0),
                                MovementType.INCOME,
                                new BigDecimal("600.00"),
                                new BigDecimal("700.00"),
                                "225487");

                when(movementService.create(any(CreateMovementRequest.class))).thenReturn(response);

                ResultActions result = mockMvc.perform(
                                post(MOVEMENTS_URL)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(DEPOSIT_JSON));

                result.andExpect(status().isCreated())
                                .andExpect(header().string("Location", "/api/movimientos/1"))
                                .andExpect(jsonPath("$.tipoMovimiento").value("Ingreso"))
                                .andExpect(jsonPath("$.valor").value(600.00))
                                .andExpect(jsonPath("$.saldo").value(700.00))
                                .andExpect(jsonPath("$.numeroCuenta").value("225487"));
        }

        @Test
        @DisplayName("POST /api/movimientos returns 409 with 'Saldo no disponible' on insufficient funds")
        void create_shouldReturn409_whenInsufficientFunds() throws Exception {
                when(movementService.create(any(CreateMovementRequest.class)))
                                .thenThrow(new InsufficientFundsException());

                ResultActions result = mockMvc.perform(
                                post(MOVEMENTS_URL)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(WITHDRAWAL_JSON));

                result.andExpect(status().isConflict())
                                .andExpect(jsonPath("$.status").value(409))
                                .andExpect(jsonPath("$.message").value("Saldo no disponible"));
        }

        @Test
        @DisplayName("POST /api/movimientos returns 400 when amount is zero")
        void create_shouldReturn400_whenAmountIsZero() throws Exception {
                when(movementService.create(any(CreateMovementRequest.class)))
                                .thenThrow(new IllegalArgumentException("valor: no puede ser cero"));

                mockMvc.perform(
                                post(MOVEMENTS_URL)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(ZERO_AMOUNT_JSON))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.message").value("valor: no puede ser cero"));
        }

        @Test
        @DisplayName("PUT /api/movimientos/{id} returns 200 with replacement movement")
        void replace_shouldReturn200() throws Exception {
                MovementResponse res = new MovementResponse(
                                3L, MOVEMENT_DATE, MovementType.EXPENSE, new BigDecimal("-300.00"),
                                new BigDecimal("1700.00"), "478758");
                when(movementService.replace(eq(10L), any(UpdateMovementRequest.class))).thenReturn(res);

                mockMvc.perform(
                                put(MOVEMENTS_URL + "/10")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(UPDATE_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.movimientoId").value(3))
                                .andExpect(jsonPath("$.valor").value(-300.00))
                                .andExpect(jsonPath("$.saldo").value(1700.00));
        }

        @Test
        @DisplayName("PUT /api/movimientos/{id} returns 409 when movement already corrected")
        void replace_shouldReturn409_whenAlreadyCorrected() throws Exception {
                when(movementService.replace(eq(10L), any(UpdateMovementRequest.class)))
                                .thenThrow(new MovementAlreadyCorrectedException(10L));

                mockMvc.perform(
                                put(MOVEMENTS_URL + "/10")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(UPDATE_JSON))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.status").value(409));
        }

        @Test
        @DisplayName("PATCH /api/movimientos/{id} delegates to replace and returns 200")
        void patch_shouldReturn200() throws Exception {
                MovementResponse res = new MovementResponse(
                                3L, MOVEMENT_DATE, MovementType.EXPENSE, new BigDecimal("-300.00"),
                                new BigDecimal("1700.00"), "478758");
                when(movementService.replace(eq(10L), any(UpdateMovementRequest.class))).thenReturn(res);

                mockMvc.perform(
                                patch(MOVEMENTS_URL + "/10")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(UPDATE_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.movimientoId").value(3))
                                .andExpect(jsonPath("$.valor").value(-300.00));
        }

        @Test
        @DisplayName("DELETE /api/movimientos/{id} returns 204 No Content")
        void compensate_shouldReturn204() throws Exception {
                doNothing().when(movementService).compensate(20L);

                mockMvc.perform(delete(MOVEMENTS_URL + "/20"))
                                .andExpect(status().isNoContent());

                verify(movementService).compensate(20L);
        }
}
