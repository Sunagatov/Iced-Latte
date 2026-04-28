package com.zufar.icedlatte.security.api;

import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.security.converter.RegistrationDtoConverter;
import com.zufar.icedlatte.security.exception.UserRegistrationException;
import com.zufar.icedlatte.security.jwt.JwtBlacklistService;
import com.zufar.icedlatte.security.jwt.JwtTokenProvider;
import com.zufar.icedlatte.user.entity.Authority;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.entity.UserGrantedAuthority;
import com.zufar.icedlatte.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRegistrationService unit tests")
class UserRegistrationServiceTest {

    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private UserRepository userRepository;
    @Mock private RegistrationDtoConverter registrationDtoConverter;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthSessionService authSessionService;
    @Mock private JwtBlacklistService jwtBlacklistService;
    @Mock private HttpServletRequest request;

    @InjectMocks private UserRegistrationService service;

    @Test
    @DisplayName("register normalizes input, persists the user, and returns a session-bound token pair")
    void registerNormalizesInputPersistsUserAndReturnsTokenPair() {
        UserRegistrationRequest registrationRequest = new UserRegistrationRequest();
        registrationRequest.setEmail("  Mixed.Case@Example.COM ");
        registrationRequest.setPassword("raw-password");

        UserEntity mappedUser = UserEntity.builder()
                .firstName("Alice")
                .lastName("Example")
                .oauthUser(false)
                .build();
        UUID userId = UUID.randomUUID();
        String refreshToken = "refresh-token";
        String refreshHash = "refresh-hash";
        String accessToken = "access-token";

        when(registrationDtoConverter.toEntity(registrationRequest)).thenReturn(mappedUser);
        when(passwordEncoder.encode("raw-password")).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity saved = invocation.getArgument(0);
            saved.setId(userId);
            return saved;
        });
        when(jwtTokenProvider.generateRefreshToken(any(UserEntity.class), any(UUID.class))).thenReturn(refreshToken);
        when(jwtBlacklistService.sha256(refreshToken)).thenReturn(refreshHash);
        when(jwtTokenProvider.generateToken(any(UserEntity.class), any(UUID.class))).thenReturn(accessToken);

        UserAuthenticationResponse response = service.register(registrationRequest, request);

        ArgumentCaptor<UserEntity> savedUserCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).saveAndFlush(savedUserCaptor.capture());
        UserEntity savedUser = savedUserCaptor.getValue();

        assertThat(savedUser.getEmail()).isEqualTo("mixed.case@example.com");
        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.isAccountNonExpired()).isTrue();
        assertThat(savedUser.isAccountNonLocked()).isTrue();
        assertThat(savedUser.isCredentialsNonExpired()).isTrue();
        assertThat(savedUser.isEnabled()).isTrue();
        assertThat(savedUser.getAuthorities())
                .singleElement()
                .extracting(UserGrantedAuthority::getAuthority)
                .isEqualTo(Authority.USER.name());

        ArgumentCaptor<UUID> sessionIdCaptor = ArgumentCaptor.forClass(UUID.class);
        verify(jwtTokenProvider).generateRefreshToken(eq(savedUser), sessionIdCaptor.capture());
        verify(authSessionService).createSession(sessionIdCaptor.getValue(), userId, refreshHash, request);
        verify(jwtTokenProvider).generateToken(savedUser, sessionIdCaptor.getValue());

        assertThat(response.getToken()).isEqualTo(accessToken);
        assertThat(response.getRefreshToken()).isEqualTo(refreshToken);
    }

    @Test
    @DisplayName("register translates duplicate-email persistence failures")
    void registerTranslatesDuplicateEmailPersistenceFailures() {
        UserRegistrationRequest registrationRequest = new UserRegistrationRequest();
        registrationRequest.setEmail("duplicate@example.com");
        registrationRequest.setPassword("raw-password");

        when(registrationDtoConverter.toEntity(registrationRequest)).thenReturn(UserEntity.builder().build());
        when(passwordEncoder.encode("raw-password")).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(UserEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> service.register(registrationRequest, request))
                .isInstanceOf(UserRegistrationException.class)
                .hasMessage("Registration failed.");

        verifyNoInteractions(jwtTokenProvider, authSessionService, jwtBlacklistService);
    }
}
