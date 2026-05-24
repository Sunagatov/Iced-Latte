package com.zufar.icedlatte.security.oauth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zufar.icedlatte.security.oauth.config.OAuthProvider;
import com.zufar.icedlatte.security.oauth.entity.OAuthIdentityEntity;

public interface OAuthIdentityRepository extends JpaRepository<OAuthIdentityEntity, UUID> {

    Optional<OAuthIdentityEntity> findByProviderAndProviderSubject(OAuthProvider provider, String providerSubject);
}
