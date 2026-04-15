package com.example.demologin.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "userId")
    private User user;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(unique = true)
    private String jti;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(length = 100)
    private String deviceId;

    @Column(length = 150)
    private String deviceName;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 255)
    private String userAgent;

    private LocalDateTime issuedAt;

    private LocalDateTime revokedAt;

    @Column(length = 255)
    private String revokedReason;

    @ManyToOne
    @JoinColumn(name = "replaced_by_token_id")
    private RefreshToken replacedByToken;

    @Column(nullable = false)
    private boolean isRevoked = false;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
