package com.kenc921.dxsp.simple_banking_service.data;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customer_bank_account_transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerBankAccountTransaction {
  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_iban", nullable = false)
  private CustomerBankAccount account;

  @Column(name = "amount", nullable = false)
  private BigDecimal amount;

  @Column(name = "currency", nullable = false, length = 3)
  private String currency;

  @Column(name = "value_date", nullable = false)
  private OffsetDateTime valueDate;

  @Column(name = "description")
  private String description;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "transaction_direction", nullable = false)
  private TransactionDirection transactionDirection;
}
