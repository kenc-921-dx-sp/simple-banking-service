package com.kenc921.dxsp.simple_banking_service.customer.repository;

import com.kenc921.dxsp.simple_banking_service.data.Customer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

  @Override
  @EntityGraph(attributePaths = {"roles", "roles.privileges"})
  Optional<Customer> findById(UUID id);
}
