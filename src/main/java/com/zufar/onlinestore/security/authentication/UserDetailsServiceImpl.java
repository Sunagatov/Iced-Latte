package com.zufar.onlinestore.security.authentication;

import com.zufar.onlinestore.user.entity.UserEntity;
import com.zufar.onlinestore.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findUserByUsername(username);
        if (user == null) {
            log.warn("Failed to get the user with the username = {}.", username);
            throw new UsernameNotFoundException(username);
        }
        return new User(
                user.getUsername(),
                passwordEncoder.encode(user.getPassword()),
                user.getAuthorities()
        );
    }
}