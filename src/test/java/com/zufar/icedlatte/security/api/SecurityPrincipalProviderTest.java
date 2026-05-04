package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.common.exception.UnauthorizedException;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.converter.UserDtoConverter;
import com.zufar.icedlatte.user.entity.UserEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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

    @Nested
    @DisplayName("get")
    class Get {

        @Test
        @DisplayName("returns converted dto for authenticated user")
        void returnsConvertedDtoForAuthenticatedUser() {
            UserEntity user = authenticatedUser();
            UserDto dto = new UserDto().id(user.getId());
            when(userDtoConverter.toDto(user)).thenReturn(dto);

            UserDto result = provider.get();

            assertThat(result).isSameAs(dto);
            verify(userDtoConverter).toDto(user);
            verifyNoMoreInteractions(userDtoConverter);
        }

        @Test
        @DisplayName("throws when authentication is missing")
        void throwsWhenAuthenticationIsMissing() {
            SecurityContextHolder.clearContext();

            assertThatThrownBy(() -> provider.get())
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Authentication required.");
        }
    }

    @Nested
    @DisplayName("getUserId")
    class GetUserId {

        @Test
        @DisplayName("returns id for authenticated user")
        void returnsIdForAuthenticatedUser() {
            UserEntity user = authenticatedUser();

            UUID result = provider.getUserId();

            assertThat(result).isEqualTo(user.getId());
            verifyNoMoreInteractions(userDtoConverter);
        }

        @Test
        @DisplayName("throws when principal is not a UserEntity")
        void throwsWhenPrincipalIsNotUserEntity() {
            SecurityContextHolder.getContext()
                    .setAuthentication(new UsernamePasswordAuthenticationToken("not-a-user", null));

            assertThatThrownBy(() -> provider.getUserId())
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Authentication required.");
        }
    }

    private static UserEntity authenticatedUser() {
        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).build();
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        return user;
    }
}
