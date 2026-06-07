package com.kenc921.dxsp.simple_banking_service.transactions.model.mapper;

import com.kenc921.dxsp.simple_banking_service.data.CustomerBankAccountTransaction;
import com.kenc921.dxsp.simple_banking_service.quote.model.QuoteDto;
import com.kenc921.dxsp.simple_banking_service.transactions.model.CustomerBankAccountTransactionView;
import com.kenc921.dxsp.simple_banking_service.transactions.model.CustomerBankTransactionViewPageable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface CustomerBankAccountTransactionViewMapper {

  @Mapping(target = "accountAlias", source = "transaction.account.account_alias")
  @Mapping(target = "accountIban", source = "transaction.account.iban")
  @Mapping(target = "majorDisplayCurrency", source = "majorDisplayCurrency")
  @Mapping(
      target = "majorDisplayCurrencyAmount",
      expression =
          "java(calculateMajorDisplayCurrencyAmount("
              + "transaction, majorDisplayCurrency, sellCurrencyQuotesForBuyCurrency))")
  CustomerBankAccountTransactionView toView(
      CustomerBankAccountTransaction transaction,
      String majorDisplayCurrency,
      Map<String, QuoteDto> sellCurrencyQuotesForBuyCurrency);

  default BigDecimal calculateMajorDisplayCurrencyAmount(
      CustomerBankAccountTransaction transaction,
      String majorDisplayCurrency,
      Map<String, QuoteDto> sellCurrencyQuotesForBuyCurrency) {
    if (transaction.getCurrency().equals(majorDisplayCurrency)) {
      return transaction.getAmount().setScale(2, RoundingMode.HALF_UP);
    }

    QuoteDto quote = sellCurrencyQuotesForBuyCurrency.get(transaction.getCurrency());
    if (quote == null) {
      throw new IllegalArgumentException(
          "Missing exchange-rate quote for "
              + transaction.getCurrency()
              + " to "
              + majorDisplayCurrency);
    }

    if (!transaction.getCurrency().equals(quote.getSellCurrency())
        || !majorDisplayCurrency.equals(quote.getBuyCurrency())) {
      throw new IllegalArgumentException("Exchange-rate quote currency pair does not match");
    }

    return transaction.getAmount().multiply(quote.getRate()).setScale(2, RoundingMode.HALF_UP);
  }

  @Mapping(target = "majorDisplayCurrency", source = "aggregatedMajorDisplayCurrency")
  @Mapping(target = "empty", source = "page.empty")
  @Mapping(target = "pageTotalCredit", source = "pageTotalCredit")
  @Mapping(target = "pageTotalDebit", source = "pageTotalDebit")
  CustomerBankTransactionViewPageable toPage(
      Page<CustomerBankAccountTransactionView> page,
      String aggregatedMajorDisplayCurrency,
      BigDecimal pageTotalCredit,
      BigDecimal pageTotalDebit);
}
