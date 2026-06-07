package com.kenc921.dxsp.simple_banking_service.config.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("banking-service.security.jwt")
public class JwtConfiguration {

  private String publicKey;
  private String privateKey;
}
