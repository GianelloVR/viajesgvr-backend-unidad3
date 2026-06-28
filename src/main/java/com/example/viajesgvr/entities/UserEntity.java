package com.example.viajesgvr.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity //esta no es una clase cualquiera, es del tipo Entity. Puede ser asociada a una tabla que esté en la base de datos
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    public static final String ROLE_CLIENT = "CLIENT";
    public static final String ROLE_ADMIN = "ADMIN";

    @Id  //Similar al primary key en las bases de datos
    @GeneratedValue(strategy = GenerationType.IDENTITY)  //autoincremental automático
    @Column(unique = true, nullable = false)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String phone;
    private String nationality;
    private String documentNumber;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false)
    private String role;
}