package com.zufar.icedlatte.security.signup.verification;

import com.zufar.icedlatte.security.session.dto.TokenPurpose;

record EmailTokenEntry(
        String email, EmailRegistrationPayload registration, TokenPurpose purpose, String encodedPassword) {}
