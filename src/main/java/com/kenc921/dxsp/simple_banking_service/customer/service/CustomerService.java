package com.kenc921.dxsp.simple_banking_service.customer.service;

import com.kenc921.dxsp.simple_banking_service.customer.repository.CustomerRepository;
import com.kenc921.dxsp.simple_banking_service.data.Customer;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerService {
  private final CustomerRepository customerRepository;

  public Customer getCustomerByCustomerId(UUID customerId) {
    return customerRepository.findById(customerId).orElseThrow();
  }
}
