package com.zufar.onlinestore.security.authentication;

import com.zufar.onlinestore.user.api.UserApi;
import com.zufar.onlinestore.user.converter.UserDtoConverter;
import com.zufar.onlinestore.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
	private final UserApi userApi;
    private final UserDtoConverter userDtoConverter;

    @Override
    public User loadUserByUsername(final String userName) throws UsernameNotFoundException {
        UserDto userDto = userApi.getUserByUserName(userName);
        return userDtoConverter.toUser(userDto);
    }
}