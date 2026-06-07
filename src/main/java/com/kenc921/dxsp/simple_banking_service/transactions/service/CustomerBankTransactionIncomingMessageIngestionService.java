package com.kenc921.dxsp.simple_banking_service.transactions.service;

import com.kenc921.dxsp.simple_banking_service.data.CustomerBankAccount;
import com.kenc921.dxsp.simple_banking_service.data.CustomerBankAccountTransaction;
import com.kenc921.dxsp.simple_banking_service.transactions.model.CustomerBankTransactionIncomingMessageDto;
import com.kenc921.dxsp.simple_banking_service.transactions.model.mapper.CustomerBankAccountTransactionMapper;
import com.kenc921.dxsp.simple_banking_service.transactions.repository.CustomerBankAccountRepository;
import com.kenc921.dxsp.simple_banking_service.transactions.repository.CustomerBankAccountTransactionRepository;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerBankTransactionIncomingMessageIngestionService {

  private final CustomerBankAccountRepository accountRepository;
  private final CustomerBankAccountTransactionRepository transactionRepository;
  private final CustomerBankAccountTransactionMapper transactionMapper;

  @Transactional
  public void ingest(CustomerBankTransactionIncomingMessageDto message) {
    if (message.amount().signum() == 0) {
      throw new IllegalArgumentException("Transaction amount must not be zero");
    }
    if (transactionRepository.existsById(message.transactionId())) {
      throw new IllegalArgumentException("Transaction already exists: " + message.transactionId());
    }

    CustomerBankAccount account =
        accountRepository
            .findById(message.accountIban())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Customer bank account not found: " + message.accountIban()));

    CustomerBankAccountTransaction transaction =
        transactionMapper.toEntity(message, account, OffsetDateTime.now());

    transactionRepository.save(transaction);
  }
}
