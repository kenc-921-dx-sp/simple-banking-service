package com.kenc921.dxsp.simple_banking_service.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

@ExtendWith(MockitoExtension.class)
class DummyJwtGeneratorTest {

  private static final String PUBLIC_KEY =
      """
      -----BEGIN PUBLIC KEY-----
      MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAoiUzvWvR1R+7IWNnXZHB
      gruVN//ZM7zkLLMp2V75m5NNO4T365yEspyqWEwItf/PajXZJ6Z++FzGqzDhBaO8
      pEVKeISvaV35L8kAe4dIHB/8M85pOqXxTybhOUiII/pP/4h9OVfb6EGT4kuOp9Sh
      GRrCMJ9WCLHS1Rh86aLDrrZ2vxjZDCdk9s4RC5WkOVrO1WfCMtbuIB9/BtbFzvEK
      HCFFC/kg+UcFFa9cbz4x1iWs442mwdV3eDfOUZFGHJ80Ii/WcV3iFm5L+KmF7PHF
      6T6DkcwQkOIqsLDW05AbGoA2CJ5jCCOQzqpLwV2Y1CC+zPVAN0qV4Kqk20sEw+RC
      SwIDAQAB
      -----END PUBLIC KEY-----
      """;

  private static final String PRIVATE_KEY =
      """
      -----BEGIN PRIVATE KEY-----
      MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCiJTO9a9HVH7sh
      Y2ddkcGCu5U3/9kzvOQssynZXvmbk007hPfrnISynKpYTAi1/89qNdknpn74XMar
      MOEFo7ykRUp4hK9pXfkvyQB7h0gcH/wzzmk6pfFPJuE5SIgj+k//iH05V9voQZPi
      S46n1KEZGsIwn1YIsdLVGHzposOutna/GNkMJ2T2zhELlaQ5Ws7VZ8Iy1u4gH38G
      1sXO8QocIUUL+SD5RwUVr1xvPjHWJazjjabB1Xd4N85RkUYcnzQiL9ZxXeIWbkv4
      qYXs8cXpPoORzBCQ4iqwsNbTkBsagDYInmMII5DOqkvBXZjUIL7M9UA3SpXgqqTb
      SwTD5EJLAgMBAAECggEAGD5KuvmE1q0CDDnHdIs8rTYsge0hmyWU0jAkQ4l6oFhz
      tZX/LD3AmM9pfX6PgN1+NCmtTcs29HM0Cy2UP4Ud8/ZzmOBx37sQF8/MYCvidr4Q
      4JLDU2ReAlEptKhpHplTCRMYchChDdwcPJDChB/RHyahPqbcNLxDGInVnL3cLIzY
      4uRpICZ+mzNHf0lZ38pckMA51NbXdCkr1JUBMHdH/tiujPV3kMb0G8A3qbezmr2j
      DGSoRt1mOQA56KmXKh/I4XDiY1rpyShU6JV2BPAcPJpobwCk8rd7uhkiWhrHse2Z
      alZhzOq8SNmS+BUFif+r4KX7/4HuSDIyvS//ZWczUQKBgQDWwgHQX1zhhDLI+jjb
      cJQUx95c3XId1COj5RiARpqcXJ7X1RZdwgb3sJu6qLLX0DFAO74SH3EKeAOwb8X9
      NwSk4i2EOX3AaE9uV2gOf7CRc05x30gcLh4oiQA2/ZhfI/67cHDbccbo09QvoqLX
      JGDiVIUdUOM2Ub5cEWaMxhOaOwKBgQDBSKRFxbhtiUuVrkfmxLQHa7huyPOLAeWs
      9PxbNFlcMVaYUEQs6Lq14KjUG6/NLFTOoCV2xJTpm/hIaJs4/DQklQp0fR4ngRpo
      3Azj9rTjgUxxnGcI8QjAJqp8dJ/xzEMDLRlM7Xskz1Pz4w8Zi9a3ManD9txDt9uf
      TVQMfeJnMQKBgFwrUZn7g6JqGV22Pna3n2Y3zZBvng19QXqS1WwgYTTgb2/UTVAZ
      +OGPE5cN1gvXl3uo6E9g3SQQFA1CO2gMl9qoE77e6cCNRCHoM4mddctHAJDQsmMd
      y+W7vrLbLe0PRsFtZZJZB3RfjX6QP3E0dLxOZ/8H+ywR8zASa7/ZNwqHAoGAJwSx
      FNy7RHGI7qiGH1HqPe1DCCpM7+zsHqQ+JXNF7tSO9KSPfrlkp764lkkYjkS6whOW
      PImmvhocXxGu9CwForTrrWRsp/DqEe0KNSLilWLOucinDCkMaS3lEMbCWx+vD38V
      MnBmgaHRAtT9gVy3dbfy23qEMK93CIulwffPsqECgYAB6c4GJmMItt6eE1vLusDh
      D5wtF8H1n+q4X697nPgcYZozhio/jE0W7KRQKWD/j8u1/zhaL74ZaiXyIyNE8Mws
      fw5+XwjdENLIY+Wz6qnPWarTUqNP5HoxRX3OpEDX/ugsjHJ1/54fcknjgl9goOV4
      qP+sM4bwKJCExIjHbSa9vw==
      -----END PRIVATE KEY-----
      """;

  @Mock private JwtConfiguration jwtConfiguration;

  private JwtSecurityConfiguration jwtSecurityConfiguration;

  @BeforeEach
  void setUp() {
    doReturn(PUBLIC_KEY).when(jwtConfiguration).getPublicKey();
    doReturn(PRIVATE_KEY).when(jwtConfiguration).getPrivateKey();
    jwtSecurityConfiguration = new JwtSecurityConfiguration();
  }

  @Test
  void generateDummyJwt() {
    UUID customerId =
        UUID.fromString(System.getProperty("customerId", "10000000-0000-0000-0000-000000000001"));

    String token = generateJwt(customerId);

    JwtDecoder decoder =
        jwtSecurityConfiguration.jwtDecoder(
            jwtSecurityConfiguration.jwtPublicKey(jwtConfiguration));
    Jwt decoded = decoder.decode(token);

    assertThat(decoded.getClaimAsString("customer_id")).isEqualTo(customerId.toString());

    System.out.println();
    System.out.println("Bearer token:");
    System.out.println(token);
    System.out.println();
  }

  private String generateJwt(UUID customerId) {
    JwtEncoder encoder =
        jwtSecurityConfiguration.jwtEncoder(
            jwtSecurityConfiguration.jwtPublicKey(jwtConfiguration),
            jwtSecurityConfiguration.jwtPrivateKey(jwtConfiguration));

    Instant issuedAt = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer("banking-service-local")
            .subject(customerId.toString())
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plus(1, ChronoUnit.HOURS))
            .claim("customer_id", customerId.toString())
            .build();
    JwsHeader headers = JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build();

    return encoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
  }
}
