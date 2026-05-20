package com.zufar.icedlatte.security.entity;

import com.zufar.icedlatte.common.audit.AuditableEntity;
import com.zufar.icedlatte.security.service.oauth.OAuthProvider;
import com.zufar.icedlatte.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "oauth_identities")
public class OAuthIdentityEntity extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 32)
    private OAuthProvider provider;

    @Column(name = "provider_subject", nullable = false)
    private String providerSubject;

    @Column(name = "email", nullable = false, length = 254)
    private String email;
}
