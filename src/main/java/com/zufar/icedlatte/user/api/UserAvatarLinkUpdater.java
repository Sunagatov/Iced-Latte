package com.zufar.icedlatte.user.api;

import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.api.avatar.UserAvatarLinkProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserAvatarLinkUpdater {

    private final UserAvatarLinkProvider userAvatarLinkProvider;

    public UserDto update(final UserDto userDto) {
        userDto.setAvatarLink(userAvatarLinkProvider.getLink(userDto.getId()));
        return userDto;
    }
}
