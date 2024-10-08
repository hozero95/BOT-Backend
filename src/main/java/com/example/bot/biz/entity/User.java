package com.example.bot.biz.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * User Entity
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "t_user")
public final class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;  // 유저 ID

    @Column(name = "email")
    private String email; // 이메일

    @Column(name = "password")
    private String password; // 비밀번호

    @Column(name = "name")
    private String name; // 이름

    @Column(name = "role")
    private String role; // 권한
}