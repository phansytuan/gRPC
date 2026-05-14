package com.example.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity persisted to the in-memory H2 database.
 * In a real service this would map to a PostgreSQL / MySQL table.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private int age;
}
