package com.zufar.onlinestore.security.converter;

import com.zufar.onlinestore.security.dto.authentication.RegistrationRequest;
import com.zufar.onlinestore.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class RegistrationDtoConverter {

    private final PasswordEncoder passwordEncoder;


    public UserDto toDto(final RegistrationRequest registrationRequest) {
        String encodedPassword = passwordEncoder.encode(registrationRequest.password());

        return new UserDto(
                null,
                registrationRequest.firstName(),
                registrationRequest.lastName(),
                registrationRequest.userName(),
                registrationRequest.email(),
                encodedPassword,
                registrationRequest.address()
        );
    }

    public User toUser(RegistrationRequest registrationRequest) {
        SimpleGrantedAuthority userAuthority = new SimpleGrantedAuthority("User");
        final Set<GrantedAuthority> authorities = Set.of(userAuthority);
        return new User(
                registrationRequest.userName(),
                passwordEncoder.encode(registrationRequest.password()),
                authorities
        );
    }
}
