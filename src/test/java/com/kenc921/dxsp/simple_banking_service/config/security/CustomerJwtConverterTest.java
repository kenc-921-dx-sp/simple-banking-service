package com.kenc921.dxsp.simple_banking_service.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kenc921.dxsp.simple_banking_service.customer.repository.CustomerRepository;
import com.kenc921.dxsp.simple_banking_service.data.Customer;
import com.kenc921.dxsp.simple_banking_service.data.CustomerStatus;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

class CustomerJwtConverterTest {

  private static final UUID CUSTOMER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

  @Test
  void authenticatesActiveCustomer() {
    CustomerJwtConverter converter = converterFor(CustomerStatus.ACTIVE);

    UsernamePasswordAuthenticationToken authentication =
        (UsernamePasswordAuthenticationToken) converter.convert(jwt(CUSTOMER_ID.toString()));

    assertThat(authentication.isAuthenticated()).isTrue();
    assertThat(authentication.getPrincipal()).isInstanceOf(CustomerUserDetails.class);
    assertThat(((CustomerUserDetails) authentication.getPrincipal()).isEnabled()).isTrue();
  }

  @Test
  void rejectsInactiveCustomer() {
    CustomerJwtConverter converter = converterFor(CustomerStatus.INACTIVE);

    assertThatThrownBy(() -> converter.convert(jwt(CUSTOMER_ID.toString())))
        .isInstanceOf(DisabledException.class)
        .hasMessage("Customer account is inactive");
  }

  @Test
  void rejectsJwtWithoutCustomerId() {
    CustomerJwtConverter converter = converterFor(CustomerStatus.ACTIVE);

    assertThatThrownBy(() -> converter.convert(jwt(null)))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessage("JWT is missing required customer_id claim");
  }

  private static CustomerJwtConverter converterFor(CustomerStatus status) {
    Customer customer = new Customer();
    customer.setId(CUSTOMER_ID);
    customer.setCustomerReference("P-0123456789");
    customer.setStatus(status);

    CustomerRepository customerRepository =
        proxy(
            CustomerRepository.class,
            (method, args) -> {
              if (method.getName().equals("findById")) {
                return Optional.of(customer);
              }
              throw new UnsupportedOperationException(method.getName());
            });

    return new CustomerJwtConverter(new HttpUserDetailsService(customerRepository));
  }

  private static Jwt jwt(String customerId) {
    Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "none").issuedAt(Instant.now());
    if (customerId != null) {
      builder.claim(CustomerJwtConverter.CUSTOMER_ID, customerId);
    }
    return builder.build();
  }

  @SuppressWarnings("unchecked")
  private static <T> T proxy(Class<T> type, RepositoryInvocation invocation) {
    return (T)
        Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[] {type},
            (proxy, method, args) -> invocation.invoke(method, args));
  }

  @FunctionalInterface
  private interface RepositoryInvocation {
    Object invoke(java.lang.reflect.Method method, Object[] args);
  }
}
