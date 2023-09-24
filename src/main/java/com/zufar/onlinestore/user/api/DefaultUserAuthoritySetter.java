package com.zufar.onlinestore.user.api;

import com.zufar.onlinestore.user.entity.Authority;
import com.zufar.onlinestore.user.entity.UserEntity;
import com.zufar.onlinestore.user.entity.UserGrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class DefaultUserAuthoritySetter {

    private static final Authority DEFAULT_USER_AUTHORITY = Authority.USER;

    public void setDefaultAuthority(final UserEntity savedUserEntity) {
        UserGrantedAuthority defaultAuthority = UserGrantedAuthority
                .builder()
                .authority(DEFAULT_USER_AUTHORITY)
                .build();

        savedUserEntity.addAuthority(defaultAuthority);
    }
}
