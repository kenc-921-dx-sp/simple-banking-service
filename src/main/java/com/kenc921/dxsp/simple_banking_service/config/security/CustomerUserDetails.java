package com.kenc921.dxsp.simple_banking_service.config.security;

import com.kenc921.dxsp.simple_banking_service.data.CustomerStatus;
import java.util.Collection;
import java.util.UUID;
import lombok.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Value
public class CustomerUserDetails implements UserDetails {

  UUID customerId;
  String customerReference;
  CustomerStatus status;
  Collection<? extends GrantedAuthority> authorities;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return "";
  }

  @Override
  public String getUsername() {
    return customerId.toString();
  }

  @Override
  public boolean isEnabled() {
    return CustomerStatus.ACTIVE.equals(status);
  }
}
