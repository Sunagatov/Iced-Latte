package com.zufar.onlinestore.user.api;

import com.zufar.onlinestore.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetPasswordByEmail {

    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public String getPasswordByEmail(String email) {
        String password = userRepository.findPasswordByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        System.out.println(password);
        return password;
    }
}
