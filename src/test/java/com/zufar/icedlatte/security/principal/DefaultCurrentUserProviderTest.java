package com.zufar.icedlatte.security.principal;

import com.zufar.icedlatte.common.audit.Identifiable;
import com.zufar.icedlatte.common.exception.UnauthorizedException;
import com.zufar.icedlatte.security.api.dto.CurrentUserSnapshot;
import com.zufar.icedlatte.user.api.UserLookupApi;
import com.zufar.icedlatte.user.api.dto.UserLookupSnapshot;
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
@DisplayName("CurrentUserProvider unit tests")
class DefaultCurrentUserProviderTest {

    @Mock
    private UserLookupApi userLookupApi;

    @InjectMocks
    private DefaultCurrentUserProvider provider;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("get")
    class Get {

        @Test
        @DisplayName("returns snapshot for authenticated user")
        void returnsSnapshotForAuthenticatedUser() {
            TestPrincipal principal = authenticatedUser();
            var user = new UserLookupSnapshot(principal.getId(), "Ada", "Lovelace", "ada@example.com");
            when(userLookupApi.getUserById(principal.getId())).thenReturn(user);

            CurrentUserSnapshot result = provider.get();

            assertThat(result.id()).isEqualTo(principal.getId());
            assertThat(result.email()).isEqualTo("ada@example.com");
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
