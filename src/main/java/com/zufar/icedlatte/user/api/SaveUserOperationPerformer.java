package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.security.converter.RegistrationDtoConverter;
import com.zufar.icedlatte.security.dto.UserRegistrationRequest;
import com.zufar.icedlatte.user.converter.UserDtoConverter;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SaveUserOperationPerformer {

    private final UserRepository userCrudRepository;
    private final RegistrationDtoConverter registrationDtoConverter;
    private final UserDtoConverter userDtoConverter;
    private final DefaultUserEntityValuesSetter defaultUserEntityValuesSetter;
    private final PasswordEncoder passwordEncoder;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public UserDto saveUser(final UserRegistrationRequest userRegistrationRequest) {
        UserEntity userEntity = registrationDtoConverter.toEntity(userRegistrationRequest);

        String encodedPassword = passwordEncoder.encode(userRegistrationRequest.password());
        userEntity.setPassword(encodedPassword);

        defaultUserEntityValuesSetter.setDefaultValues(userEntity);
        UserEntity userEntityWithId = userCrudRepository.save(userEntity);
        return userDtoConverter.toDto(userEntityWithId);
    }
}
