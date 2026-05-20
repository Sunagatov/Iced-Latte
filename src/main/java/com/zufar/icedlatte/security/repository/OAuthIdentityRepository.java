package com.zufar.icedlatte.security.repository;

import com.zufar.icedlatte.security.entity.OAuthIdentityEntity;
import com.zufar.icedlatte.security.service.oauth.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OAuthIdentityRepository extends JpaRepository<OAuthIdentityEntity, UUID> {

    Optional<OAuthIdentityEntity> findByProviderAndProviderSubject(OAuthProvider provider,
                                                                   String providerSubject);
}
