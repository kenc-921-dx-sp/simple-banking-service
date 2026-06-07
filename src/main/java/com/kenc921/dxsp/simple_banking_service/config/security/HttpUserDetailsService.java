package com.kenc921.dxsp.simple_banking_service.config.security;

import com.kenc921.dxsp.simple_banking_service.customer.repository.CustomerRepository;
import com.kenc921.dxsp.simple_banking_service.data.Customer;
import com.kenc921.dxsp.simple_banking_service.data.CustomerPrivilege;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HttpUserDetailsService implements UserDetailsService {

  private final CustomerRepository customerRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    UUID customerId;
    try {
      customerId = UUID.fromString(username);
    } catch (IllegalArgumentException exception) {
      throw new UsernameNotFoundException("Invalid customer ID", exception);
    }

    Customer customer =
        customerRepository
            .findById(customerId)
            .orElseThrow(() -> new UsernameNotFoundException("Customer not found: " + customerId));

    Set<GrantedAuthority> authorities =
        customer.getRoles().stream()
            .flatMap(role -> role.getPrivileges().stream())
            .map(CustomerPrivilege::getPrivilegeName)
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toUnmodifiableSet());

    return new CustomerUserDetails(
        customer.getId(), customer.getCustomerReference(), customer.getStatus(), authorities);
  }
}
