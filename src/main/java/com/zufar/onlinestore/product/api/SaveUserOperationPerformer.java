package com.zufar.onlinestore.product.api;

import com.zufar.onlinestore.user.api.DefaultUserAuthoritySetter;
import com.zufar.onlinestore.user.converter.UserDtoConverter;
import com.zufar.onlinestore.user.dto.UserDto;
import com.zufar.onlinestore.user.entity.UserEntity;
import com.zufar.onlinestore.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SaveUserOperationPerformer {

    private final UserRepository userCrudRepository;
    private final UserDtoConverter userDtoConverter;
    private final DefaultUserAuthoritySetter defaultUserAuthoritySetter;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public UserDto saveUser(final UserDto userDto) {
        UserEntity userEntity = userDtoConverter.toEntity(userDto);
        defaultUserAuthoritySetter.setDefaultValues(userEntity);
        UserEntity userEntityWithId = userCrudRepository.save(userEntity);
        return userDtoConverter.toDto(userEntityWithId);
    }
}
