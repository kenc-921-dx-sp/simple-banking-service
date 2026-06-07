package com.kenc921.dxsp.simple_banking_service.transactions.model;

import com.kenc921.dxsp.simple_banking_service.data.TransactionDirection;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CustomerBankAccountTransactionView {
  UUID id;
  BigDecimal amount;
  String currency;
  OffsetDateTime valueDate;
  String description;
  OffsetDateTime createdAt;
  TransactionDirection transactionDirection;
  String accountAlias;
  String accountIban;
  String majorDisplayCurrency;
  BigDecimal majorDisplayCurrencyAmount;
}
