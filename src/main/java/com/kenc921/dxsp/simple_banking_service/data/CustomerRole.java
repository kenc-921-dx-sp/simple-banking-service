package com.kenc921.dxsp.simple_banking_service.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customer_role")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRole {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false, updatable = false)
  private Integer id;

  @Enumerated(EnumType.STRING)
  @Column(name = "role_name", nullable = false, unique = true, length = 100)
  private CustomerRoleName roleName;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "customer_role_customer_privilege",
      joinColumns = @JoinColumn(name = "customer_role_id"),
      inverseJoinColumns = @JoinColumn(name = "customer_privilege_id"))
  private Set<CustomerPrivilege> privileges = new HashSet<>();
}
