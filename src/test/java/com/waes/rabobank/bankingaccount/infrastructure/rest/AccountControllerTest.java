package com.waes.rabobank.bankingaccount.infrastructure.rest;

import com.waes.rabobank.bankingaccount.application.dto.AccountBalanceDTO;
import com.waes.rabobank.bankingaccount.application.service.AccountService;
import com.waes.rabobank.bankingaccount.application.service.WithdrawalService;
import com.waes.rabobank.bankingaccount.infrastructure.config.JpaAuditingConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
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
    void getAllAccounts_shouldReturnBalances() throws Exception {
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
        Mockito.when(accountService.getBalancesByUserId(eq(userId))).thenReturn(balances);

        mockMvc.perform(get("/api/accounts")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountId").value("acc-1"))
                .andExpect(jsonPath("$[0].balance").value(100.0))
                .andExpect(jsonPath("$[1].accountId").value("acc-2"))
                .andExpect(jsonPath("$[1].balance").value(200.0));
    }

    @Test
    void getAccount_shouldReturnAccountResponseDTO() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/api/accounts/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(id.toString()))
                .andExpect(jsonPath("$.balance").value(1000.50));
    }

    @Test
    void getAccountsCount_shouldReturnCount() throws Exception {
        mockMvc.perform(get("/api/accounts/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("600489147"));
    }

//    @Test
//    void withdraw_shouldReturnWithdrawalResponseDTO() throws Exception {
//        String accountId = "acc-123";
//        WithdrawalRequestDTO request = new WithdrawalRequestDTO(accountId, BigDecimal.valueOf(50));
//        WithdrawalResponseDTO response = new WithdrawalResponseDTO(accountId, BigDecimal.valueOf(950.0), "SUCCESS");
//
//        Mockito.when(withdrawalService.withdraw(any(WithdrawalRequestDTO.class))).thenReturn(response);
//
//        String requestBody = """
//            {
//                "accountId": "acc-123",
//                "amount": 50
//            }
//            """;
//
//        mockMvc.perform(post("/api/accounts/acc-123/withdraw")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(requestBody))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.accountId").value("acc-123"))
//                .andExpect(jsonPath("$.balance").value(950.0))
//                .andExpect(jsonPath("$.status").value("SUCCESS"));
//    }
//
//    @Test
//    void withdraw_shouldReturnBadRequestForAccountIdMismatch() throws Exception {
//        String requestBody = """
//            {
//                "accountId": "wrong-id",
//                "amount": 50
//            }
//            """;
//
//        mockMvc.perform(post("/api/accounts/acc-123/withdraw")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(requestBody))
//                .andExpect(status().isBadRequest());
//    }
}
