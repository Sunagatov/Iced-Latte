package com.zufar.icedlatte.security.signup.verification;

import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.security.session.dto.TokenPurpose;

record EmailTokenEntry(UserRegistrationRequest request, TokenPurpose purpose, String encodedPassword) {}
