package com.kenc921.dxsp.simple_banking_service.transactions.model;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Value
@Builder
public class CustomerBankTransactionViewPageable {
  List<CustomerBankAccountTransactionView> content;
  Pageable pageable;
  long totalElements;
  int totalPages;
  boolean last;
  int size;
  int number;
  Sort sort;
  boolean first;
  int numberOfElements;
  boolean empty;
  String majorDisplayCurrency;
  BigDecimal pageTotalCredit;
  BigDecimal pageTotalDebit;
}
