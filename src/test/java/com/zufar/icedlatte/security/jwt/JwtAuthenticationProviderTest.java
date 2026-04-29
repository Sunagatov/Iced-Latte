package com.zufar.icedlatte.security.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationProvider Tests")
class JwtAuthenticationProviderTest {

    @Mock
    private JwtTokenFromAuthHeaderExtractor jwtTokenFromAuthHeaderExtractor;

    @Mock
    private JwtClaimExtractor jwtClaimExtractor;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private JwtBlacklistValidator jwtBlacklistValidator;

    @InjectMocks
    private JwtAuthenticationProvider jwtAuthenticationProvider;

    @Nested
    @DisplayName("get")
    class Get {

        @Test
        @DisplayName("successfully authenticates user and attaches request details")
        void successfullyAuthenticatesUserAndAttachesRequestDetails() {
            MockHttpServletRequest httpRequest = new MockHttpServletRequest();
            httpRequest.setRemoteAddr("203.0.113.10");
            UserDetails userDetails = new User("test@example.com", "password", Collections.emptyList());
            String jwtToken = "mockJwtToken";
            String userEmail = "test@example.com";

            when(jwtTokenFromAuthHeaderExtractor.extract(httpRequest)).thenReturn(jwtToken);
            when(jwtClaimExtractor.extractEmail(jwtToken)).thenReturn(userEmail);
            when(userDetailsService.loadUserByUsername(userEmail)).thenReturn(userDetails);

            var authenticationToken = jwtAuthenticationProvider.get(httpRequest);

            assertThat(authenticationToken.getPrincipal()).isEqualTo(userDetails);
            assertThat(authenticationToken.getCredentials()).isNull();
            assertThat(authenticationToken.getDetails()).isInstanceOf(WebAuthenticationDetails.class);
            verify(jwtTokenFromAuthHeaderExtractor).extract(httpRequest);
            verify(jwtBlacklistValidator).validate(jwtToken);
            verify(jwtClaimExtractor).extractEmail(jwtToken);
            verify(userDetailsService).loadUserByUsername(userEmail);
            verifyNoMoreInteractions(jwtTokenFromAuthHeaderExtractor, jwtBlacklistValidator, jwtClaimExtractor, userDetailsService);
        }
    }
}
