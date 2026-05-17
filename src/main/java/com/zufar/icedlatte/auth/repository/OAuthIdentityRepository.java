package com.zufar.icedlatte.auth.repository;

import com.zufar.icedlatte.auth.api.OAuthProvider;
import com.zufar.icedlatte.auth.entity.OAuthIdentityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OAuthIdentityRepository extends JpaRepository<OAuthIdentityEntity, UUID> {

    Optional<OAuthIdentityEntity> findByProviderAndProviderSubject(OAuthProvider provider,
                                                                   String providerSubject);
}
