package com.kenc921.dxsp.simple_banking_service.transactions.model.mapper;

import com.kenc921.dxsp.simple_banking_service.data.CustomerBankAccount;
import com.kenc921.dxsp.simple_banking_service.data.CustomerBankAccountTransaction;
import com.kenc921.dxsp.simple_banking_service.data.TransactionDirection;
import com.kenc921.dxsp.simple_banking_service.transactions.model.CustomerBankTransactionIncomingMessageDto;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface CustomerBankAccountTransactionMapper {

  @Mapping(target = "id", source = "message.transactionId")
  @Mapping(target = "customer", source = "account.customer")
  @Mapping(target = "account", source = "account")
  @Mapping(target = "amount", source = "message.amount")
  @Mapping(target = "currency", source = "message.currency")
  @Mapping(target = "valueDate", source = "message.valueDate")
  @Mapping(target = "description", source = "message.description")
  @Mapping(target = "createdAt", source = "createdAt")
  @Mapping(
      target = "transactionDirection",
      source = "message.amount",
      qualifiedByName = "mapTransactionDirection")
  CustomerBankAccountTransaction toEntity(
      CustomerBankTransactionIncomingMessageDto message,
      CustomerBankAccount account,
      OffsetDateTime createdAt);

  @Named("mapTransactionDirection")
  default TransactionDirection mapTransactionDirection(BigDecimal amount) {
    return amount.signum() > 0 ? TransactionDirection.CREDIT : TransactionDirection.DEBIT;
  }
}
