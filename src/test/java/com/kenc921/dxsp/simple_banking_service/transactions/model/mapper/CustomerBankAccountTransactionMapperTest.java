package com.kenc921.dxsp.simple_banking_service.transactions.model.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.kenc921.dxsp.simple_banking_service.data.Customer;
import com.kenc921.dxsp.simple_banking_service.data.CustomerBankAccount;
import com.kenc921.dxsp.simple_banking_service.data.CustomerBankAccountTransaction;
import com.kenc921.dxsp.simple_banking_service.data.TransactionDirection;
import com.kenc921.dxsp.simple_banking_service.transactions.model.CustomerBankTransactionIncomingMessageDto;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class CustomerBankAccountTransactionMapperTest {

  private static final CustomerBankAccountTransactionMapper MAPPER =
      Mappers.getMapper(CustomerBankAccountTransactionMapper.class);
  private static final UUID TRANSACTION_ID =
      UUID.fromString("30000000-0000-0000-0000-000000000001");
  private static final String IBAN = "GB29NWBK60161331926819";
  private static final OffsetDateTime VALUE_DATE = OffsetDateTime.parse("2026-02-01T09:30:00Z");
  private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-02-01T09:31:00Z");

  @Test
  void mapsIncomingCreditTransaction() {
    Customer customer = new Customer();
    CustomerBankAccount account = account(customer);
    CustomerBankTransactionIncomingMessageDto message = message(new BigDecimal("125.50"));

    CustomerBankAccountTransaction transaction = MAPPER.toEntity(message, account, CREATED_AT);

    assertThat(transaction.getId()).isEqualTo(TRANSACTION_ID);
    assertThat(transaction.getCustomer()).isSameAs(customer);
    assertThat(transaction.getAccount()).isSameAs(account);
    assertThat(transaction.getAmount()).isEqualByComparingTo("125.50");
    assertThat(transaction.getCurrency()).isEqualTo("USD");
    assertThat(transaction.getValueDate()).isEqualTo(VALUE_DATE);
    assertThat(transaction.getDescription()).isEqualTo("Kafka test transaction");
    assertThat(transaction.getCreatedAt()).isEqualTo(CREATED_AT);
    assertThat(transaction.getTransactionDirection()).isEqualTo(TransactionDirection.CREDIT);
  }

  @Test
  void mapsIncomingDebitTransaction() {
    CustomerBankAccountTransaction transaction =
        MAPPER.toEntity(message(new BigDecimal("-25.00")), account(new Customer()), CREATED_AT);

    assertThat(transaction.getTransactionDirection()).isEqualTo(TransactionDirection.DEBIT);
  }

  private static CustomerBankAccount account(Customer customer) {
    CustomerBankAccount account = new CustomerBankAccount();
    account.setIban(IBAN);
    account.setCustomer(customer);
    return account;
  }

  private static CustomerBankTransactionIncomingMessageDto message(BigDecimal amount) {
    return new CustomerBankTransactionIncomingMessageDto(
        TRANSACTION_ID, IBAN, amount, "USD", VALUE_DATE, "Kafka test transaction");
  }
}
