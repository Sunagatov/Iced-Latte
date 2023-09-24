package com.zufar.onlinestore.user.api;

import com.zufar.onlinestore.user.entity.Authority;
import com.zufar.onlinestore.user.entity.UserEntity;
import com.zufar.onlinestore.user.entity.UserGrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class AuthorityService {

    public void setDefaultAuthority(UserEntity savedUserEntity) {
        UserGrantedAuthority defaultAuthority = UserGrantedAuthority
                .builder()
                .authority(Authority.USER)
                .build();

        savedUserEntity.addAuthority(defaultAuthority);
    }
}
