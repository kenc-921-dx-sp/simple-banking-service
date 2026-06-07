package com.kenc921.dxsp.simple_banking_service.transactions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kenc921.dxsp.simple_banking_service.data.Customer;
import com.kenc921.dxsp.simple_banking_service.data.CustomerBankAccount;
import com.kenc921.dxsp.simple_banking_service.data.CustomerBankAccountTransaction;
import com.kenc921.dxsp.simple_banking_service.data.TransactionDirection;
import com.kenc921.dxsp.simple_banking_service.transactions.model.CustomerBankTransactionIncomingMessageDto;
import com.kenc921.dxsp.simple_banking_service.transactions.model.mapper.CustomerBankAccountTransactionMapper;
import com.kenc921.dxsp.simple_banking_service.transactions.repository.CustomerBankAccountRepository;
import com.kenc921.dxsp.simple_banking_service.transactions.repository.CustomerBankAccountTransactionRepository;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class CustomerBankTransactionIncomingMessageIngestionServiceTest {

  private static final String IBAN = "GB29NWBK60161331926819";

  @Test
  void ingestsCreditAndMapsAccountCustomer() {
    Customer customer = new Customer();
    CustomerBankAccount account = new CustomerBankAccount();
    account.setIban(IBAN);
    account.setCustomer(customer);
    AtomicReference<CustomerBankAccountTransaction> saved = new AtomicReference<>();

    CustomerBankTransactionIncomingMessageIngestionService service =
        new CustomerBankTransactionIncomingMessageIngestionService(
            accountRepositoryReturning(account),
            transactionRepository(false, saved),
            transactionMapper());
    OffsetDateTime before = OffsetDateTime.now();

    service.ingest(message(new BigDecimal("125.50")));

    CustomerBankAccountTransaction transaction = saved.get();
    assertThat(transaction.getAccount()).isSameAs(account);
    assertThat(transaction.getCustomer()).isSameAs(customer);
    assertThat(transaction.getAmount()).isEqualByComparingTo("125.50");
    assertThat(transaction.getTransactionDirection()).isEqualTo(TransactionDirection.CREDIT);
    assertThat(transaction.getCreatedAt()).isAfterOrEqualTo(before);
  }

  @Test
  void ingestsNegativeAmountAsDebit() {
    CustomerBankAccount account = new CustomerBankAccount();
    account.setIban(IBAN);
    account.setCustomer(new Customer());
    AtomicReference<CustomerBankAccountTransaction> saved = new AtomicReference<>();
    CustomerBankTransactionIncomingMessageIngestionService service =
        new CustomerBankTransactionIncomingMessageIngestionService(
            accountRepositoryReturning(account),
            transactionRepository(false, saved),
            transactionMapper());

    service.ingest(message(new BigDecimal("-25.00")));

    assertThat(saved.get().getTransactionDirection()).isEqualTo(TransactionDirection.DEBIT);
  }

  @Test
  void rejectsZeroAmountAndDuplicateTransaction() {
    CustomerBankTransactionIncomingMessageIngestionService service =
        new CustomerBankTransactionIncomingMessageIngestionService(
            accountRepositoryReturning(new CustomerBankAccount()),
            transactionRepository(true, new AtomicReference<>()),
            transactionMapper());

    assertThatThrownBy(() -> service.ingest(message(BigDecimal.ZERO)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Transaction amount must not be zero");
    assertThatThrownBy(() -> service.ingest(message(BigDecimal.ONE)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Transaction already exists");
  }

  private static CustomerBankTransactionIncomingMessageDto message(BigDecimal amount) {
    return new CustomerBankTransactionIncomingMessageDto(
        UUID.fromString("30000000-0000-0000-0000-000000000001"),
        IBAN,
        amount,
        "USD",
        OffsetDateTime.parse("2026-02-01T09:30:00Z"),
        "Kafka test transaction");
  }

  private static CustomerBankAccountRepository accountRepositoryReturning(
      CustomerBankAccount account) {
    return proxy(
        CustomerBankAccountRepository.class,
        (method, args) -> {
          if (method.getName().equals("findById")) {
            return Optional.of(account);
          }
          throw new UnsupportedOperationException(method.getName());
        });
  }

  private static CustomerBankAccountTransactionRepository transactionRepository(
      boolean exists, AtomicReference<CustomerBankAccountTransaction> saved) {
    return proxy(
        CustomerBankAccountTransactionRepository.class,
        (method, args) -> {
          if (method.getName().equals("existsById")) {
            return exists;
          }
          if (method.getName().equals("save")) {
            CustomerBankAccountTransaction transaction = (CustomerBankAccountTransaction) args[0];
            saved.set(transaction);
            return transaction;
          }
          throw new UnsupportedOperationException(method.getName());
        });
  }

  private static CustomerBankAccountTransactionMapper transactionMapper() {
    return Mappers.getMapper(CustomerBankAccountTransactionMapper.class);
  }

  @SuppressWarnings("unchecked")
  private static <T> T proxy(Class<T> type, RepositoryInvocation invocation) {
    return (T)
        Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[] {type},
            (proxy, method, args) -> invocation.invoke(method, args));
  }

  @FunctionalInterface
  private interface RepositoryInvocation {
    Object invoke(java.lang.reflect.Method method, Object[] args);
  }
}
