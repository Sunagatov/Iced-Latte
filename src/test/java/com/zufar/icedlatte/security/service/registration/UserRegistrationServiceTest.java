package com.zufar.icedlatte.security.service.registration;

import com.zufar.icedlatte.security.service.session.SessionTokenService;

import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.security.converter.RegistrationDtoConverter;
import com.zufar.icedlatte.security.exception.UserRegistrationException;
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

    @Mock private UserRepository userRepository;
    @Mock private RegistrationDtoConverter registrationDtoConverter;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private SessionTokenService sessionTokenService;
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
        UserAuthenticationResponse tokenPair = new UserAuthenticationResponse();
        tokenPair.setToken("access-token");
        tokenPair.setRefreshToken("refresh-token");

        when(registrationDtoConverter.toEntity(registrationRequest)).thenReturn(mappedUser);
        when(passwordEncoder.encode("raw-password")).thenReturn("encoded-password");
        when(userRepository.saveAndFlush(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity saved = invocation.getArgument(0);
            saved.setId(java.util.UUID.randomUUID());
            return saved;
        });
        when(sessionTokenService.issueForNewSession(any(UserEntity.class), eq(request)))
                .thenReturn(tokenPair);

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
        verify(sessionTokenService).issueForNewSession(savedUser, request);

        assertThat(response.getToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("ensureEmailAvailable rejects existing normalized email")
    void ensureEmailAvailableRejectsExistingNormalizedEmail() {
        UserRegistrationRequest registrationRequest = new UserRegistrationRequest();
        registrationRequest.setEmail("  Duplicate@Example.COM ");
        when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.ensureEmailAvailable(registrationRequest))
                .isInstanceOf(UserRegistrationException.class)
                .hasMessage("This email is already registered. Please sign in or use a different email.");

        verify(userRepository).existsByEmail("duplicate@example.com");
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
                .hasMessage("This email is already registered. Please sign in or use a different email.");

        verifyNoInteractions(sessionTokenService);
    }
}
