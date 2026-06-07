package com.kenc921.dxsp.simple_banking_service.config.security;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration(proxyBeanMethods = false)
public class JwtSecurityConfiguration {

  @Bean
  RSAPublicKey jwtPublicKey(JwtConfiguration configuration) {
    try {
      byte[] key = decodePem(configuration.getPublicKey(), "PUBLIC KEY");
      return (RSAPublicKey)
          KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(key));
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to parse JWT public key", exception);
    }
  }

  @Bean
  @Profile("local")
  RSAPrivateKey jwtPrivateKey(JwtConfiguration configuration) {
    try {
      byte[] key = decodePem(configuration.getPrivateKey(), "PRIVATE KEY");
      return (RSAPrivateKey)
          KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(key));
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to parse JWT private key", exception);
    }
  }

  @Bean
  JwtDecoder jwtDecoder(RSAPublicKey publicKey) {
    return NimbusJwtDecoder.withPublicKey(publicKey).build();
  }

  @Bean
  @Profile("local")
  JwtEncoder jwtEncoder(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
    return NimbusJwtEncoder.withKeyPair(publicKey, privateKey).build();
  }

  private static byte[] decodePem(String pem, String keyType) {
    if (pem == null || pem.isBlank()) {
      throw new IllegalArgumentException("JWT " + keyType + " is not configured");
    }

    String encoded =
        pem.replace("-----BEGIN " + keyType + "-----", "")
            .replace("-----END " + keyType + "-----", "")
            .replaceAll("\\s", "");

    return Base64.getDecoder().decode(encoded);
  }
}
