package com.waes.rabobank.bankingaccount.infrastructure.rest;

import com.waes.rabobank.bankingaccount.application.dto.AccountBalanceDTO;
import com.waes.rabobank.bankingaccount.application.dto.WithdrawalRequestDTO;
import com.waes.rabobank.bankingaccount.application.dto.WithdrawalResponseDTO;
import com.waes.rabobank.bankingaccount.application.service.AccountService;
import com.waes.rabobank.bankingaccount.application.service.WithdrawalService;
import com.waes.rabobank.bankingaccount.shared.exception.InsufficientFundsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private WithdrawalService withdrawalService;

    @Test
    void shouldReturnBalancesWhenGettingAllAccounts() throws Exception {
        UUID userId = UUID.randomUUID();
        List<AccountBalanceDTO> balances = List.of(
                new AccountBalanceDTO(
                        userId.toString(),
                        "acc-1",
                        "NL01RABO0123456789",
                        BigDecimal.valueOf(100.0),
                        "EUR"
                ),
                new AccountBalanceDTO(
                        "user-2",
                        "acc-2",
                        "NL01RABO0987654321",
                        BigDecimal.valueOf(200.0),
                        "EUR"
                )
        );
        when(accountService.getBalancesByUserId(eq(userId))).thenReturn(balances);

        mockMvc.perform(get("/api/accounts")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountId").value("acc-1"))
                .andExpect(jsonPath("$[0].balance").value(100.0))
                .andExpect(jsonPath("$[1].accountId").value("acc-2"))
                .andExpect(jsonPath("$[1].balance").value(200.0));
    }

    @Test
    void shouldReturnAccountWhenGettingById() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/api/accounts/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(id.toString()))
                .andExpect(jsonPath("$.balance").value(1000.50));
    }

    @Test
    void shouldReturnCountWhenGettingAccountsCount() throws Exception {
        mockMvc.perform(get("/api/accounts/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("600489147"));
    }

    // === Withdrawal Endpoint Tests ===
    @Test
    void shouldWithdrawSuccessfully() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();

        WithdrawalResponseDTO response = new WithdrawalResponseDTO(
                accountId.toString(),
                cardId.toString(),
                new BigDecimal("100.00"),
                new BigDecimal("1.00"),
                new BigDecimal("899.00")
        );

        when(withdrawalService.withdraw(any(WithdrawalRequestDTO.class)))
                .thenReturn(response);

        String requestBody = String.format("""
        {
            "accountId": "%s",
            "amount": 100.00,
            "cardId": "%s"
        }
        """, accountId, cardId);

        mockMvc.perform(post("/api/accounts/" + accountId + "/withdraw")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.cardId").value(cardId.toString()))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.fee").value(1.00))
                .andExpect(jsonPath("$.newBalance").value(899.00));
    }

    @Test
    void shouldReturn422WhenInsufficientFunds() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();

        when(withdrawalService.withdraw(any(WithdrawalRequestDTO.class)))
                .thenThrow(new InsufficientFundsException(
                        accountId,
                        new BigDecimal("50.00"),
                        new BigDecimal("100.00")
                ));

        String requestBody = String.format("""
        {
            "accountId": "%s",
            "amount": 100.00,
            "cardId": "%s"
        }
        """, accountId, cardId);

        mockMvc.perform(post("/api/accounts/" + accountId + "/withdraw")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.title").value("Insufficient Funds"))
                .andExpect(jsonPath("$.availableBalance").value(50.00))
                .andExpect(jsonPath("$.requestedAmount").value(100.00))
                .andExpect(jsonPath("$.shortfall").value(50.00));
    }

    @Test
    void shouldReturn400WhenRequestIsInvalid() throws Exception {
        UUID accountId = UUID.randomUUID();

        String requestBody = """
        {
            "accountId": "",
            "amount": -10,
            "cardId": ""
        }
        """;

        mockMvc.perform(post("/api/accounts/" + accountId + "/withdraw")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}