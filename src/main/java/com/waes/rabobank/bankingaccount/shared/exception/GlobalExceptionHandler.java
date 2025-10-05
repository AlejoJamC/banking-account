package com.waes.rabobank.bankingaccount.shared.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

import static net.logstash.logback.argument.StructuredArguments.kv;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ============================
    // === 404 Not Found Errors ===
    // ============================
    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail handleAccountNotFoundException(AccountNotFoundException ex) {
        logger.warn("exception.account_not_found", kv("accountId", ex.getAccountId()));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problem.setTitle("Account Not Found");
        problem.setType(URI.create("https://api.rabobank.com/errors/account-not-found"));
        problem.setProperty("accountId", ex.getAccountId().toString());

        return problem;
    }

    @ExceptionHandler(CardNotFoundException.class)
    public ProblemDetail handleCardNotFoundException(CardNotFoundException ex) {
        logger.warn("exception.card_not_found", kv("cardId", ex.getCardId()));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problem.setTitle("Card Not Found");
        problem.setType(URI.create("https://api.rabobank.com/errors/card-not-found"));
        problem.setProperty("cardId", ex.getCardId().toString());

        return problem;
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFoundException(UserNotFoundException ex) {
        logger.warn("exception.user_not_found", kv("userId", ex.getUserId()));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problem.setTitle("User Not Found");
        problem.setType(URI.create("https://api.rabobank.com/errors/user-not-found"));
        problem.setProperty("userId", ex.getUserId().toString());

        return problem;
    }

    // ============================
    // === 422 Business Rule Violations ===
    // ============================
    @ExceptionHandler(InsufficientFundsException.class)
    public ProblemDetail handleInsufficientFundsException(InsufficientFundsException ex) {
        logger.warn("exception.insufficient_funds",
                kv("accountId", ex.getAccountId()),
                kv("availableBalance", ex.getAvailableBalance()),
                kv("requestedAmount", ex.getRequestedAmount()),
                kv("shortfall", ex.getShortfall())
        );

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage()
        );
        problem.setTitle("Insufficient Funds");
        problem.setType(URI.create("https://api.rabobank.com/errors/insufficient-funds"));
        problem.setProperty("accountId", ex.getAccountId().toString());
        problem.setProperty("availableBalance", ex.getAvailableBalance());
        problem.setProperty("requestedAmount", ex.getRequestedAmount());
        problem.setProperty("shortfall", ex.getShortfall());

        return problem;
    }

    @ExceptionHandler(CardAccountMismatchException.class)
    public ProblemDetail handleCardAccountMismatchException(CardAccountMismatchException ex) {
        logger.warn("exception.card_account_mismatch",
                kv("cardId", ex.getCardId()),
                kv("accountId", ex.getAccountId())
        );

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage()
        );
        problem.setTitle("Card Account Mismatch");
        problem.setType(URI.create("https://api.rabobank.com/errors/card-account-mismatch"));
        problem.setProperty("cardId", ex.getCardId().toString());
        problem.setProperty("accountId", ex.getAccountId().toString());

        return problem;
    }

    @ExceptionHandler(InactiveCardException.class)
    public ProblemDetail handleInactiveCardException(InactiveCardException ex) {
        logger.warn("exception.inactive_card", kv("cardId", ex.getCardId()));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage()
        );
        problem.setTitle("Inactive Card");
        problem.setType(URI.create("https://api.rabobank.com/errors/inactive-card"));
        problem.setProperty("cardId", ex.getCardId().toString());

        return problem;
    }

    @ExceptionHandler(InactiveAccountException.class)
    public ProblemDetail handleInactiveAccountException(InactiveAccountException ex) {
        logger.warn("exception.inactive_account", kv("accountId", ex.getAccountId()));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage()
        );
        problem.setTitle("Inactive Account");
        problem.setType(URI.create("https://api.rabobank.com/errors/inactive-account"));
        problem.setProperty("accountId", ex.getAccountId().toString());

        return problem;
    }

    @ExceptionHandler(SelfTransferException.class)
    public ProblemDetail handleSelfTransferException(SelfTransferException ex) {
        logger.warn("exception.self_transfer", kv("accountId", ex.getAccountId()));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage()
        );
        problem.setTitle("Self Transfer Not Allowed");
        problem.setType(URI.create("https://api.rabobank.com/errors/self-transfer"));
        problem.setProperty("accountId", ex.getAccountId().toString());

        return problem;
    }

    // ============================
    // === 400 Bad Request ===
    // ============================
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("exception.illegal_argument", kv("message", ex.getMessage()));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problem.setTitle("Invalid Request");
        problem.setType(URI.create("https://api.rabobank.com/errors/bad-request"));

        return problem;
    }

    @ExceptionHandler(AccountIdMismatchException.class)
    public ProblemDetail handleAccountIdMismatchException(AccountIdMismatchException ex) {
        logger.warn("exception.account_id_mismatch",
                kv("pathAccountId", ex.getPathAccountId()),
                kv("bodyAccountId", ex.getBodyAccountId())
        );

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problem.setTitle("Account ID Mismatch");
        problem.setType(URI.create("https://api.rabobank.com/errors/account-id-mismatch"));
        problem.setProperty("pathAccountId", ex.getPathAccountId());
        problem.setProperty("bodyAccountId", ex.getBodyAccountId());

        return problem;
    }
}