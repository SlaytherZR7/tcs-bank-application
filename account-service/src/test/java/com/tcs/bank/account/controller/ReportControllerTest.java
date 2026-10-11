package com.tcs.bank.account.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcs.bank.account.exceptions.ClientProjectionNotFoundException;
import com.tcs.bank.account.model.dto.AccountStatementReportItem;
import com.tcs.bank.account.model.enumeration.AccountType;
import com.tcs.bank.account.service.ReportService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReportController.class)
@Import(AccountExceptionHandler.class)
class ReportControllerTest {

    private static final String REPORT_URL = "/api/reportes";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @Test
    @DisplayName("GET /api/reportes returns 200 with JSON matching technical test schema using fechaInicio/fechaFin")
    void getReport_shouldReturn200_whenStandardParamsProvided() throws Exception {
        AccountStatementReportItem item = new AccountStatementReportItem(
                "10/2/2022",
                "Marianela Montalvo",
                "225487",
                AccountType.CHECKING,
                new BigDecimal("100.00"),
                true,
                new BigDecimal("600.00"),
                new BigDecimal("700.00"));

        when(reportService.generateAccountStatement(
                eq(2L),
                eq(LocalDate.of(2022, 2, 1)),
                eq(LocalDate.of(2022, 2, 28))))
                .thenReturn(List.of(item));

        mockMvc.perform(get(REPORT_URL)
                .param("clienteId", "2")
                .param("fechaInicio", "2022-02-01")
                .param("fechaFin", "2022-02-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].Fecha").value("10/2/2022"))
                .andExpect(jsonPath("$[0].Cliente").value("Marianela Montalvo"))
                .andExpect(jsonPath("$[0]['Numero Cuenta']").value("225487"))
                .andExpect(jsonPath("$[0].Tipo").value("Corriente"))
                .andExpect(jsonPath("$[0]['Saldo Inicial']").value(100.00))
                .andExpect(jsonPath("$[0].Estado").value(true))
                .andExpect(jsonPath("$[0].Movimiento").value(600.00))
                .andExpect(jsonPath("$[0]['Saldo Disponible']").value(700.00));
    }

    @Test
    @DisplayName("GET /api/reportes returns 400 when fechaFin is missing")
    void getReport_shouldReturn400_whenFechaFinMissing() throws Exception {
        mockMvc.perform(get(REPORT_URL)
                .param("clienteId", "2")
                .param("fechaInicio", "2022-02-10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("fechaFin: es obligatoria"))
                .andExpect(jsonPath("$.path").value(REPORT_URL));
    }

    @Test
    @DisplayName("GET /api/reportes returns 400 when a date is not ISO formatted")
    void getReport_shouldReturn400_whenDateIsInvalid() throws Exception {
        mockMvc.perform(get(REPORT_URL)
                .param("clienteId", "2")
                .param("fechaInicio", "10/02/2022")
                .param("fechaFin", "2022-02-28"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("fechaInicio: valor inválido '10/02/2022'"));
    }

    @Test
    @DisplayName("GET /api/reportes returns 400 when fechaInicio is after fechaFin")
    void getReport_shouldReturn400_whenDateRangeInverted() throws Exception {
        mockMvc.perform(get(REPORT_URL)
                .param("clienteId", "2")
                .param("fechaInicio", "2022-02-28")
                .param("fechaFin", "2022-02-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("fechaInicio: no puede ser posterior a fechaFin"));

        verifyNoInteractions(reportService);
    }

    @Test
    @DisplayName("GET /api/reportes returns 404 when client is not found")
    void getReport_shouldReturn404_whenClientNotFound() throws Exception {
        when(reportService.generateAccountStatement(eq(99L), any(), any()))
                .thenThrow(new ClientProjectionNotFoundException(99L));

        mockMvc.perform(get(REPORT_URL)
                .param("clienteId", "99")
                .param("fechaInicio", "2022-01-01")
                .param("fechaFin", "2022-01-31"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/reportes returns 400 when clienteId is missing")
    void getReport_shouldReturn400_whenClienteIdMissing() throws Exception {
        mockMvc.perform(get(REPORT_URL)
                .param("fechaInicio", "2022-02-01")
                .param("fechaFin", "2022-02-28"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("clienteId: es obligatorio"));
    }
}
