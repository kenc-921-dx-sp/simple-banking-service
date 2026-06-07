package com.kenc921.dxsp.simple_banking_service.quote.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QuoteDto {
  String sellCurrency;
  String buyCurrency;
  BigDecimal rate;
  OffsetDateTime updatedAt;
}
