package com.zufar.icedlatte.user.entity;

import java.util.UUID;

import jakarta.persistence.*;

import org.springframework.security.core.GrantedAuthority;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user_granted_authority")
public class UserGrantedAuthority implements GrantedAuthority {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID userAuthorityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "authority", nullable = false, length = 32)
    private Authority authority;

    @Override
    public String getAuthority() {
        return authority.name();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserGrantedAuthority that)) return false;
        return userAuthorityId != null && userAuthorityId.equals(that.userAuthorityId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
