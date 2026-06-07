package com.kenc921.dxsp.simple_banking_service.transactions.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CustomerBankTransactionIncomingMessageDto(
    @NotNull UUID transactionId,
    @NotBlank String accountIban,
    @NotNull BigDecimal amount,
    @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
    @NotNull OffsetDateTime valueDate,
    @Size(max = 255) String description) {}
