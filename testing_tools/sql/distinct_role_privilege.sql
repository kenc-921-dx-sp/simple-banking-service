SELECT cr.role_name,
       cp.privilege_name
FROM customer_role_customer_privilege crcp
         JOIN customer_role cr ON crcp.customer_role_id = cr.id
         JOIN customer_privilege cp ON crcp.customer_privilege_id = cp.id