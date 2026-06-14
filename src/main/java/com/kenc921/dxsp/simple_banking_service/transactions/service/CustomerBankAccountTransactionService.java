package com.kenc921.dxsp.simple_banking_service.transactions.service;

import com.kenc921.dxsp.simple_banking_service.config.transactions.TransactionConfiguration;
import com.kenc921.dxsp.simple_banking_service.customer.service.CustomerService;
import com.kenc921.dxsp.simple_banking_service.data.Customer;
import com.kenc921.dxsp.simple_banking_service.data.CustomerBankAccountTransaction;
import com.kenc921.dxsp.simple_banking_service.data.TransactionDirection;
import com.kenc921.dxsp.simple_banking_service.quote.model.QuoteDto;
import com.kenc921.dxsp.simple_banking_service.quote.service.QuoteService;
import com.kenc921.dxsp.simple_banking_service.transactions.model.CustomerBankAccountTransactionView;
import com.kenc921.dxsp.simple_banking_service.transactions.model.CustomerBankTransactionViewPageable;
import com.kenc921.dxsp.simple_banking_service.transactions.model.mapper.CustomerBankAccountTransactionViewMapper;
import com.kenc921.dxsp.simple_banking_service.transactions.repository.CustomerBankAccountTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerBankAccountTransactionService {

  private final CustomerBankAccountTransactionRepository customerBankAccountTransactionRepository;
  private final CustomerService customerService;
  private final CustomerBankAccountTransactionViewMapper customerBankAccountTransactionViewMapper;
  private final QuoteService quoteService;
  private final TransactionConfiguration transactionConfiguration;

  public CustomerBankTransactionViewPageable getCustomerTransactions(
      UUID customerId, YearMonth yearMonth, String majorDisplayCurrency, Pageable pageable) {
    Customer customer = customerService.getCustomerByCustomerId(customerId);

    OffsetDateTime queryStartDate = yearMonth.atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC);
    OffsetDateTime queryEndDate =
        yearMonth.atEndOfMonth().atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);

    Page<CustomerBankAccountTransaction> customerBankAccountTransactions =
        customerBankAccountTransactionRepository
            .findCustomerBankAccountTransactionByCustomerIdAndValueDateRange(
                customerId, queryStartDate, queryEndDate, pageable);

    Set<String> distinctTransactionCurrencies =
        customerBankAccountTransactions.getContent().stream()
            .map(CustomerBankAccountTransaction::getCurrency)
            .collect(Collectors.toUnmodifiableSet());

    String aggregatedMajorDisplayCurrency =
        getAggregatedMajorDisplayCurrency(customer, majorDisplayCurrency);

    Map<String, QuoteDto> sellCurrencyQuotesForBuyCurrency =
        quoteService.getSellCurrencyQuotesForBuyCurrency(
            aggregatedMajorDisplayCurrency, distinctTransactionCurrencies);

    Page<CustomerBankAccountTransactionView> viewPage =
        customerBankAccountTransactions.map(
            customerBankAccountTransaction ->
                customerBankAccountTransactionViewMapper.toView(
                    customerBankAccountTransaction,
                    aggregatedMajorDisplayCurrency,
                    sellCurrencyQuotesForBuyCurrency));

    BigDecimal pageTotalDebit = extractPageTotal(viewPage, TransactionDirection.DEBIT);
    BigDecimal pageTotalCredit = extractPageTotal(viewPage, TransactionDirection.CREDIT);

    return customerBankAccountTransactionViewMapper.toPage(
        viewPage, aggregatedMajorDisplayCurrency, pageTotalCredit, pageTotalDebit);
  }

  private String getAggregatedMajorDisplayCurrency(Customer customer, String majorDisplayCurrency) {
    if (majorDisplayCurrency != null) {
      return majorDisplayCurrency;
    }
    if (customer.getMajorDisplayCurrency() != null) {
      return customer.getMajorDisplayCurrency();
    }
    return transactionConfiguration.getSystemDefaultMajorDisplayCurrency();
  }

  private BigDecimal extractPageTotal(
      Page<CustomerBankAccountTransactionView> viewPage,
      TransactionDirection transactionDirection) {
    return viewPage.getContent().stream()
        .filter(
            transactionView ->
                transactionDirection.equals(transactionView.getTransactionDirection()))
        .map(CustomerBankAccountTransactionView::getMajorDisplayCurrencyAmount)
        .filter(Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
