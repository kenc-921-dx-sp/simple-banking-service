package com.kenc921.dxsp.simple_banking_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication(scanBasePackages = "com.kenc921.dxsp.simple_banking_service")
@ConfigurationPropertiesScan(basePackages = "com.kenc921.dxsp.simple_banking_service.config")
public class BankingServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(BankingServiceApplication.class, args);
  }
}
