package com.kenc921.dxsp.simple_banking_service.transactions.repository;

import com.kenc921.dxsp.simple_banking_service.data.CustomerBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerBankAccountRepository extends JpaRepository<CustomerBankAccount, String> {}
