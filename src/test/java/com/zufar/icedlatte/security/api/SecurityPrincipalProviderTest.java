package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.converter.UserDtoConverter;
import com.zufar.icedlatte.user.entity.UserEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityPrincipalProvider unit tests")
class SecurityPrincipalProviderTest {

    @Mock
    private UserDtoConverter userDtoConverter;
    @InjectMocks
    private SecurityPrincipalProvider provider;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void setAuthenticatedUser(UserEntity user) {
        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("get() returns UserDto for authenticated user")
    void get_authenticatedUser_returnsDto() {
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).build();
        UserDto dto = new UserDto();
        setAuthenticatedUser(user);
        when(userDtoConverter.toDto(user)).thenReturn(dto);

        UserDto result = provider.get();

        assertThat(result).isEqualTo(dto);
    }

    @Test
    @DisplayName("getUserId() returns UUID for authenticated user")
    void getUserId_authenticatedUser_returnsId() {
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).build();
        setAuthenticatedUser(user);

        UUID result = provider.getUserId();

        assertThat(result).isEqualTo(userId);
    }

    @Test
    @DisplayName("getUserId() throws IllegalStateException when no authentication")
    void getUserId_noAuthentication_throwsIllegalStateException() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> provider.getUserId())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No authenticated UserEntity");
    }

    @Test
    @DisplayName("getUserId() throws IllegalStateException when principal is not UserEntity")
    void getUserId_wrongPrincipalType_throwsIllegalStateException() {
        var auth = new UsernamePasswordAuthenticationToken("not-a-user-entity", null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(() -> provider.getUserId())
                .isInstanceOf(IllegalStateException.class);
    }
}
