package com.kenc921.dxsp.simple_banking_service.config.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "banking-service.kafka", name = "enabled", havingValue = "true")
public class BankingServiceKafkaConfiguration {}
