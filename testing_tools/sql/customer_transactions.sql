SELECT cbc.iban,
       cbct.transaction_direction transaction_direction,
       cbct.currency              transaction_currency,
       cbct.amount                transaction_amount,
       cbct.value_date,
       cbct.description
FROM customer c
         LEFT JOIN customer_bank_account cbc ON cbc.customer_id = c.id
         LEFT JOIN customer_bank_account_transaction cbct ON cbct.account_iban = cbc.iban
WHERE c.customer_reference = 'P-0123456789'
ORDER BY c.id, cbct.value_date DESC
