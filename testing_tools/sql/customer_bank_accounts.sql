SELECT c.customer_name      client_name,
       c.customer_reference client_reference,
       c.status             client_status,
       cbc.iban,
       cbc.account_alias,
       cbc.account_currencies
FROM customer c
         LEFT JOIN customer_bank_account cbc ON cbc.customer_id = c.id
