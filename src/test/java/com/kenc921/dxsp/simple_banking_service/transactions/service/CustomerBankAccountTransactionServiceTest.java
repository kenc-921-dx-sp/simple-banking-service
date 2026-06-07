package com.kenc921.dxsp.simple_banking_service.transactions.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.kenc921.dxsp.simple_banking_service.config.transactions.TransactionConfiguration;
import com.kenc921.dxsp.simple_banking_service.customer.repository.CustomerRepository;
import com.kenc921.dxsp.simple_banking_service.customer.service.CustomerService;
import com.kenc921.dxsp.simple_banking_service.data.Customer;
import com.kenc921.dxsp.simple_banking_service.data.CustomerBankAccountTransaction;
import com.kenc921.dxsp.simple_banking_service.quote.service.QuoteService;
import com.kenc921.dxsp.simple_banking_service.transactions.model.CustomerBankTransactionViewPageable;
import com.kenc921.dxsp.simple_banking_service.transactions.model.mapper.CustomerBankAccountTransactionViewMapper;
import com.kenc921.dxsp.simple_banking_service.transactions.repository.CustomerBankAccountTransactionRepository;
import java.lang.reflect.Proxy;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class CustomerBankAccountTransactionServiceTest {

  private static final UUID CUSTOMER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
  private static final YearMonth YEAR_MONTH = YearMonth.of(2026, 1);

  @Test
  void requestMajorDisplayCurrencyOverridesCustomerAndSystemDefaults() {
    CustomerBankTransactionViewPageable result = getTransactions("EUR", "GBP", "USD");

    assertThat(result.getMajorDisplayCurrency()).isEqualTo("EUR");
  }

  @Test
  void customerMajorDisplayCurrencyOverridesSystemDefault() {
    CustomerBankTransactionViewPageable result = getTransactions(null, "GBP", "USD");

    assertThat(result.getMajorDisplayCurrency()).isEqualTo("GBP");
  }

  @Test
  void systemDefaultMajorDisplayCurrencyIsUsedAsFinalFallback() {
    CustomerBankTransactionViewPageable result = getTransactions(null, null, "USD");

    assertThat(result.getMajorDisplayCurrency()).isEqualTo("USD");
  }

  private static CustomerBankTransactionViewPageable getTransactions(
      String requestCurrency, String customerCurrency, String systemDefaultCurrency) {
    Customer customer = new Customer();
    customer.setMajorDisplayCurrency(customerCurrency);

    CustomerService customerService =
        new CustomerService(
            proxy(
                CustomerRepository.class,
                (method, args) -> {
                  if (method.getName().equals("findById")) {
                    return Optional.of(customer);
                  }
                  throw new UnsupportedOperationException(method.getName());
                }));

    CustomerBankAccountTransactionRepository repository =
        proxy(
            CustomerBankAccountTransactionRepository.class,
            (method, args) -> {
              if (method
                  .getName()
                  .equals("findCustomerBankAccountTranscationByCustomerIdAndValueDateRange")) {
                Pageable requestedPageable = (Pageable) args[3];
                return new PageImpl<CustomerBankAccountTransaction>(
                    List.of(), requestedPageable, 0);
              }
              throw new UnsupportedOperationException(method.getName());
            });
    Pageable pageable = PageRequest.of(0, 20);

    QuoteService quoteService = (buyCurrency, sellCurrencies) -> Map.of();

    TransactionConfiguration configuration = new TransactionConfiguration();
    configuration.setSystemDefaultMajorDisplayCurrency(systemDefaultCurrency);

    CustomerBankAccountTransactionService service =
        new CustomerBankAccountTransactionService(
            repository,
            customerService,
            Mappers.getMapper(CustomerBankAccountTransactionViewMapper.class),
            quoteService,
            configuration);

    return service.getCustomerTransactions(CUSTOMER_ID, YEAR_MONTH, requestCurrency, pageable);
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
