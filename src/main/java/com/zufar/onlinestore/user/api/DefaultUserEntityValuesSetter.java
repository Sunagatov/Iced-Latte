package com.zufar.onlinestore.user.api;

import com.zufar.onlinestore.user.entity.Authority;
import com.zufar.onlinestore.user.entity.UserEntity;
import com.zufar.onlinestore.user.entity.UserGrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class DefaultUserEntityValuesSetter {

    public static final UserGrantedAuthority DEFAULT_USER_GRANTED_AUTHORITY = UserGrantedAuthority.builder().authority(Authority.USER).build();
    private static final boolean DEFAULT_ACCOUNT_NON_EXPIRED = true;
    private static final boolean DEFAULT_ACCOUNT_NON_LOCKED = true;
    private static final boolean DEFAULT_CREDENTIALS_NON_EXPIRED = true;
    private static final boolean DEFAULT_ENABLED = true;

    public void setDefaultValues(final UserEntity savedUserEntity) {
        savedUserEntity.addAuthority(DEFAULT_USER_GRANTED_AUTHORITY);
        savedUserEntity.setAccountNonExpired(DEFAULT_ACCOUNT_NON_EXPIRED);
        savedUserEntity.setAccountNonLocked(DEFAULT_ACCOUNT_NON_LOCKED);
        savedUserEntity.setCredentialsNonExpired(DEFAULT_CREDENTIALS_NON_EXPIRED);
        savedUserEntity.setEnabled(DEFAULT_ENABLED);
    }
}
