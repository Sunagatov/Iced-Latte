package com.zufar.onlinestore.security.configuration;

import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class SpringSecurityConfigurationTest {
    @InjectMocks
    private SpringSecurityConfiguration springSecurityConfiguration;

    @Test
    void authenticationProviderTest(){
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        UserDetailsService userDetailsServiceMock = mock(UserDetailsService.class);

        when(userDetailsServiceMock.loadUserByUsername("Igor")).thenReturn(Instancio.of(UserDetails.class).create());

        authenticationProvider.setPasswordEncoder(passwordEncoder);
        authenticationProvider.setUserDetailsService(userDetailsServiceMock);

        AuthenticationProvider authenticationProvider1 = springSecurityConfiguration.authenticationProvider(userDetailsServiceMock);

    }
}
