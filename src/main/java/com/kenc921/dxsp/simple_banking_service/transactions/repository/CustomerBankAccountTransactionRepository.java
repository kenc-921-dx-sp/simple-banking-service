package com.kenc921.dxsp.simple_banking_service.transactions.repository;

import com.kenc921.dxsp.simple_banking_service.data.CustomerBankAccountTransaction;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerBankAccountTransactionRepository
    extends JpaRepository<CustomerBankAccountTransaction, UUID> {

  @Query(
      value =
          """
					select transaction
					from CustomerBankAccountTransaction transaction
					join fetch transaction.account
					where transaction.customer.id = :customerId
					  and transaction.valueDate >= :queryStartDate
					  and transaction.valueDate <= :queryEndDate
					order by transaction.valueDate desc, transaction.id desc
					""",
      countQuery =
          """
					select count(transaction)
					from CustomerBankAccountTransaction transaction
					where transaction.customer.id = :customerId
					  and transaction.valueDate >= :queryStartDate
					  and transaction.valueDate <= :queryEndDate
					""")
  Page<CustomerBankAccountTransaction>
      findCustomerBankAccountTransactionByCustomerIdAndValueDateRange(
          @Param("customerId") UUID customerId,
          @Param("queryStartDate") OffsetDateTime queryStartDate,
          @Param("queryEndDate") OffsetDateTime queryEndDate,
          Pageable pageable);
}
