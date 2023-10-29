package com.zufar.onlinestore.user.api;

import com.zufar.onlinestore.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class GetPasswordByEmailTest {
    @InjectMocks
    private GetPasswordByEmail getPasswordByEmail;
    @Mock
    private UserRepository userRepository;

    private String emailTrue;
    private String emailFalse;
    private String password;

    @BeforeEach
    void setUp() {
        emailTrue = "true";
        emailFalse = "false";
        password = "password";

        Mockito.when(userRepository.findPasswordByEmail(emailTrue))
                .thenReturn(Optional.ofNullable(password));
    }

    @Test
    @DisplayName("test getPasswordByEmail")
    public void testGetPasswordByEmail() {
        assertEquals(password, getPasswordByEmail.getPasswordByEmail(emailTrue));
        assertThrows(RuntimeException.class,
                () -> getPasswordByEmail.getPasswordByEmail(emailFalse));
    }

}