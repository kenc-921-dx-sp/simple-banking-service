package com.kenc921.dxsp.simple_banking_service.quote.service;

import com.kenc921.dxsp.simple_banking_service.quote.model.QuoteDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.Set;

public interface QuoteService {
  /**
   * Retrieves the current exchange-rate quotes required to convert multiple sell currencies into
   * one buy currency.
   *
   * <p>The returned map is keyed by sell currency. Each value describes the quote from that sell
   * currency to {@code buyCurrency}, using the convention that one unit of the sell currency is
   * worth {@link QuoteDto#getRate()} units of the buy currency. For example, a quote with {@code
   * sellCurrency=CHF}, {@code buyCurrency=USD}, and {@code rate=1.10} means that CHF 1 is converted
   * to USD 1.10.
   *
   * @param buyCurrency the target currency into which transaction amounts will be converted
   * @param sellCurrencies the source currencies for which quotes are required
   * @return a map from each available sell currency to its quote against {@code buyCurrency}
   */
  Map<String, QuoteDto> getSellCurrencyQuotesForBuyCurrency(
      @NotBlank String buyCurrency, @NotNull Set<String> sellCurrencies);
}
