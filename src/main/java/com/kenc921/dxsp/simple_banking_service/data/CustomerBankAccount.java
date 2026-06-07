package com.kenc921.dxsp.simple_banking_service.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customer_bank_account")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerBankAccount {

  @Id
  @Column(name = "iban", nullable = false, updatable = false)
  private String iban;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @Column(name = "account_alias", nullable = false)
  private String account_alias;

  @Column(name = "account_currencies", nullable = false, columnDefinition = "varchar(3)[]")
  private String[] accountCurrencies;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;
}
