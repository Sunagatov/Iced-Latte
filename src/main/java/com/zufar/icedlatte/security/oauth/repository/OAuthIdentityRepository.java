package com.zufar.icedlatte.security.oauth.repository;

import com.zufar.icedlatte.security.oauth.api.OAuthProvider;
import com.zufar.icedlatte.security.oauth.entity.OAuthIdentityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OAuthIdentityRepository extends JpaRepository<OAuthIdentityEntity, UUID> {

    Optional<OAuthIdentityEntity> findByProviderAndProviderSubject(OAuthProvider provider,
                                                                   String providerSubject);
}
