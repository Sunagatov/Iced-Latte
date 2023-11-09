package com.zufar.onlinestore.security.api;

import com.zufar.onlinestore.openapi.dto.UserDto;
import com.zufar.onlinestore.user.converter.UserDtoConverter;
import com.zufar.onlinestore.user.entity.UserEntity;
import org.instancio.Instancio;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class SecurityPrincipalProviderTest {

    @InjectMocks
    private SecurityPrincipalProvider securityPrincipalProvider;

    @Mock
    private UserDtoConverter userDtoConverter;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private UserEntity userEntity = Instancio.create(UserEntity.class);
    private UserDto userDto = Instancio.create(UserDto.class);

    private static MockedStatic<SecurityContextHolder> mockedSecurityContextHolder;

    @BeforeAll
    static void setUpOnce() {
        mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
    }

    @AfterAll
    static void tearDownOnce() {
        mockedSecurityContextHolder.close();
    }

    @BeforeEach
    void setUp() {
        mockedSecurityContextHolder.when(()->SecurityContextHolder.getContext()).thenReturn(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userEntity);
        when(userDtoConverter.toDto(userEntity)).thenReturn(userDto);
    }

    @Test
    @DisplayName("Should get User Dto from Security Context")
    void shouldGetUserDtoFromSecurityContext() {
        UserDto userDto = securityPrincipalProvider.get();

        assertNotNull(userDto);
        verify(securityContext, times(1)).getAuthentication();
        verify(authentication, times(1)).getPrincipal();
        verify(userDtoConverter, times(1)).toDto(userEntity);
    }

    @Test
    @DisplayName("Should get userId from Security Context")
    void shouldGetUserIdFromSecurityContext() {
        UUID userId = securityPrincipalProvider.getUserId();
        assertNotNull(userId);
    }
}