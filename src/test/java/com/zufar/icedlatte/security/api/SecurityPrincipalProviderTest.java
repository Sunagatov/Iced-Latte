package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.common.audit.Identifiable;
import com.zufar.icedlatte.common.exception.UnauthorizedException;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.api.UserLookupApi;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityPrincipalProvider unit tests")
class SecurityPrincipalProviderTest {

    @Mock
    private UserLookupApi userLookupApi;

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
            TestPrincipal principal = authenticatedUser();
            UserDto dto = new UserDto().id(principal.getId());
            when(userLookupApi.getUserById(principal.getId())).thenReturn(dto);

            UserDto result = provider.get();

            assertThat(result).isSameAs(dto);
            verify(userLookupApi).getUserById(principal.getId());
            verifyNoMoreInteractions(userLookupApi);
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
            TestPrincipal principal = authenticatedUser();

            UUID result = provider.getUserId();

            assertThat(result).isEqualTo(principal.getId());
            verifyNoMoreInteractions(userLookupApi);
        }

        @Test
        @DisplayName("throws when principal is not identifiable")
        void throwsWhenPrincipalIsNotIdentifiable() {
            SecurityContextHolder.getContext()
                    .setAuthentication(new UsernamePasswordAuthenticationToken("not-a-user", null));

            assertThatThrownBy(() -> provider.getUserId())
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Authentication required.");
        }
    }

    private static TestPrincipal authenticatedUser() {
        TestPrincipal user = new TestPrincipal(UUID.randomUUID());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
        return user;
    }

    private record TestPrincipal(UUID id) implements Identifiable {
        @Override
        public UUID getId() {
            return id;
        }
    }
}
