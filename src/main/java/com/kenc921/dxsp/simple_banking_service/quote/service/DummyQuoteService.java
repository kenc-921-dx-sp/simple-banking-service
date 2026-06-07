package com.kenc921.dxsp.simple_banking_service.quote.service;

import com.kenc921.dxsp.simple_banking_service.quote.model.QuoteDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * Placeholder implementation of {@link QuoteService} used to represent the external exchange-rate
 * provider assumed by the coding challenge.
 *
 * <p>The requirements in {@code requirements.md} state that exchange rates are supplied by an
 * external API, but they do not define a provider, protocol, authentication mechanism, availability
 * contract, or response format. This implementation preserves that integration boundary while
 * keeping transaction processing independent of a specific third-party vendor.
 *
 * <p>A production implementation would replace this service with an adapter that invokes the
 * selected provider and handles authentication, timeouts, retries, rate limiting, caching, and
 * error translation.
 *
 * @implNote This implementation is intentionally limited to the scope required by the exercise and
 *     must not be used as a source of production exchange rates.
 */
@Service
@Validated
public class DummyQuoteService implements QuoteService {
  private static final BigDecimal DUMMY_RATE = BigDecimal.valueOf(0.5);

  @Override
  public Map<String, QuoteDto> getSellCurrencyQuotesForBuyCurrency(
      @NotBlank String buyCurrency, @NotNull Set<String> sellCurrencies) {
    return sellCurrencies.stream()
        .map(
            sellCurrency -> {
              return QuoteDto.builder()
                  .sellCurrency(sellCurrency)
                  .buyCurrency(buyCurrency)
                  .rate(DUMMY_RATE)
                  .build();
            })
        .collect(Collectors.toUnmodifiableMap(QuoteDto::getSellCurrency, quoteDto -> quoteDto));
  }
}
