package com.kenc921.dxsp.simple_banking_service.config.transactions;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Configuration for transaction module system behavior. */
@Data
@Configuration
@ConfigurationProperties("banking-service.transactions")
public class TransactionConfiguration {

  /** Default currency used for transaction total credit and debit conversion and display. */
  private String systemDefaultMajorDisplayCurrency;
}
