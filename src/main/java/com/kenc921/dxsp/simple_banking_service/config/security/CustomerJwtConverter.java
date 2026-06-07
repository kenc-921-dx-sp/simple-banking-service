package com.kenc921.dxsp.simple_banking_service.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
final class CustomerJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  static final String CUSTOMER_ID = "customer_id";

  private final HttpUserDetailsService userDetailsService;

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    String customerId = jwt.getClaimAsString(CUSTOMER_ID);
    if (!StringUtils.hasText(customerId)) {
      throw new BadCredentialsException("JWT is missing required customer_id claim");
    }

    CustomerUserDetails customerUserDetails =
        (CustomerUserDetails) userDetailsService.loadUserByUsername(customerId);

    if (!customerUserDetails.isEnabled()) {
      throw new DisabledException("Customer account is inactive");
    }

    return UsernamePasswordAuthenticationToken.authenticated(
        customerUserDetails, jwt, customerUserDetails.getAuthorities());
  }
}
