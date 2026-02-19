package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.user.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserEntityDefaultsProvider {

    private static final boolean DEFAULT_ACCOUNT_NON_EXPIRED = true;
    private static final boolean DEFAULT_ACCOUNT_NON_LOCKED = true;
    private static final boolean DEFAULT_CREDENTIALS_NON_EXPIRED = true;
    private static final boolean DEFAULT_ENABLED = true;

    public void applyDefaults(final UserEntity userEntity) {
        if (userEntity != null) {
            userEntity.setAccountNonExpired(DEFAULT_ACCOUNT_NON_EXPIRED);
            userEntity.setAccountNonLocked(DEFAULT_ACCOUNT_NON_LOCKED);
            userEntity.setCredentialsNonExpired(DEFAULT_CREDENTIALS_NON_EXPIRED);
            userEntity.setEnabled(DEFAULT_ENABLED);
        }
    }
}