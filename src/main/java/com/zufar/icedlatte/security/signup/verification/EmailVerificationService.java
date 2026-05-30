package com.zufar.icedlatte.security.signup.verification;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;

import com.zufar.icedlatte.common.util.EmailNormalizer;
import com.zufar.icedlatte.openapi.dto.ConfirmEmailRequest;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.security.email.sender.AuthTokenEmailSender;
import com.zufar.icedlatte.security.session.dto.TokenPurpose;
import com.zufar.icedlatte.security.signup.registration.UserRegistrationService;
import com.zufar.icedlatte.user.api.UserAccessControlApi;
import com.zufar.icedlatte.user.api.UserLookupApi;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final AuthTokenEmailSender emailConfirmation;
    private final EmailTokenService emailTokenService;
    private final UserRegistrationService userRegistrationService;
    private final UserLookupApi userLookupApi;
    private final UserAccessControlApi userAccessControlApi;

    public void sendEmailVerificationCode(UserRegistrationRequest request) {
        userRegistrationService.ensureRegistrationAllowed(request);
        String token = emailTokenService.generate(request, TokenPurpose.EMAIL_VERIFICATION);
        emailConfirmation.sendTemporaryCode(EmailNormalizer.normalize(request.getEmail()), token);
    }

    public void sendPasswordResetCode(String email) {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail(EmailNormalizer.normalize(email));
        String token = emailTokenService.generate(request, TokenPurpose.PASSWORD_RESET);
        emailConfirmation.sendTemporaryCode(request.getEmail(), token);
    }

    public UserAuthenticationResponse confirmEmailByCode(
            ConfirmEmailRequest confirmEmailRequest, HttpServletRequest httpRequest) {
        EmailTokenEntry entry = emailTokenService.consume(confirmEmailRequest, TokenPurpose.EMAIL_VERIFICATION);
        if (entry.encodedPassword() == null || entry.encodedPassword().isBlank()) {
            throw new IllegalStateException("Email verification token is missing encoded password");
        }
        return userRegistrationService.completeEmailVerifiedRegistration(entry.request(), entry.encodedPassword(), httpRequest);
    }

    public void confirmResetPasswordEmailByCode(ConfirmEmailRequest confirmEmailRequest, String newPassword) {
        UserRegistrationRequest request = emailTokenService.consume(confirmEmailRequest, TokenPurpose.PASSWORD_RESET).request();
        var user = userLookupApi.getUserByEmail(request.getEmail());
        userAccessControlApi.changePassword(user.id(), newPassword);
    }
}
