package com.kenc921.dxsp.simple_banking_service.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customer_privilege")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPrivilege {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false, updatable = false)
  private Integer id;

  @Column(name = "privilege_name", nullable = false, unique = true, length = 100)
  private String privilegeName;
}
