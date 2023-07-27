package com.zufar.onlinestore.security.authentication;

import com.zufar.onlinestore.user.converter.UserDtoConverter;
import com.zufar.onlinestore.user.dto.UserDto;
import com.zufar.onlinestore.user.entity.UserEntity;
import com.zufar.onlinestore.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDtoConverter userDtoConverter;

    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findUserByUsername(username);
        if (user == null) {
            log.warn("Failed to get the user with the username = {}.", username);
            throw new UsernameNotFoundException(username);
        }
        UserDto userDto = userDtoConverter.toDto(user);
        SimpleGrantedAuthority userAuthority = new SimpleGrantedAuthority("User");
        final Set<GrantedAuthority> authorities = Set.of(userAuthority);
        return new User(
                userDto.username(),
                passwordEncoder.encode(userDto.password()),
                authorities
        );
    }
}