package com.kenc921.dxsp.simple_banking_service.transactions.controller;

import com.kenc921.dxsp.simple_banking_service.config.security.CustomerUserDetails;
import com.kenc921.dxsp.simple_banking_service.transactions.model.CustomerBankTransactionViewPageable;
import com.kenc921.dxsp.simple_banking_service.transactions.service.CustomerBankAccountTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Customer bank account transaction operations")
public class CustomerBankAccountTransactionController {
  private final CustomerBankAccountTransactionService customerBankAccountTransactionService;

  @PreAuthorize("hasAuthority('transactions:view')")
  @GetMapping
  @Operation(summary = "Get customer transactions")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Customer transactions returned",
        content =
            @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CustomerBankTransactionViewPageable.class))),
    @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
    @ApiResponse(responseCode = "401", description = "Authentication required"),
    @ApiResponse(responseCode = "403", description = "Insufficient privileges")
  })
  public ResponseEntity<CustomerBankTransactionViewPageable> getTransactions(
      @Parameter(hidden = true) @AuthenticationPrincipal CustomerUserDetails customerUserDetails,
      @Parameter(description = "Transaction year", example = "2026") @RequestParam @Min(1) int year,
      @Parameter(description = "Transaction month", example = "6") @RequestParam @Min(1) @Max(12)
          int month,
      @Parameter(description = "Zero-based page number", example = "0")
          @RequestParam(defaultValue = "0")
          @Min(0)
          int page,
      @Parameter(description = "Number of transactions per page", example = "20")
          @RequestParam(defaultValue = "20")
          @Min(1)
          int size,
      @Parameter(description = "ISO 4217 currency used for display values", example = "USD")
          @RequestParam(required = false)
          @Pattern(regexp = "[A-Z]{3}")
          String majorDisplayCurrency) {
    YearMonth yearMonth = YearMonth.of(year, month);
    Pageable pageable = PageRequest.of(page, size);

    if (yearMonth.isAfter(YearMonth.now())) {
      log.error("Invalid YearMonth, year: {}, month: {}", year, month);
      throw new IllegalArgumentException();
    }

    CustomerBankTransactionViewPageable customerBankTransactionViewPageable =
        customerBankAccountTransactionService.getCustomerTransactions(
            customerUserDetails.getCustomerId(), yearMonth, majorDisplayCurrency, pageable);

    return ResponseEntity.ok(customerBankTransactionViewPageable);
  }
}
