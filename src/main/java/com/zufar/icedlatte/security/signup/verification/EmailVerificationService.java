package com.zufar.icedlatte.security.signup.verification;

import org.springframework.stereotype.Service;

import com.zufar.icedlatte.common.util.EmailNormalizer;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.security.email.sender.AuthTokenEmailSender;
import com.zufar.icedlatte.security.session.management.AuthSessionRequestMetadata;
import com.zufar.icedlatte.security.session.token.AuthenticationTokens;
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
        String token = emailTokenService.generateEmailVerificationToken(request);
        emailConfirmation.sendTemporaryCode(EmailNormalizer.normalize(request.getEmail()), token);
    }

    public void sendPasswordResetCode(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        String token = emailTokenService.generatePasswordResetToken(normalizedEmail);
        emailConfirmation.sendTemporaryCode(normalizedEmail, token);
    }

    public AuthenticationTokens confirmEmailByCode(String token, AuthSessionRequestMetadata requestMetadata) {
        EmailVerificationTokenPayload payload = emailTokenService.consumeEmailVerificationToken(token);
        String encodedPassword = payload.encodedPassword();
        if (encodedPassword == null || encodedPassword.isBlank()) {
            throw new IllegalStateException("Email verification token is missing encoded password");
        }
        UserRegistrationRequest registrationRequest = toRegistrationRequest(payload.registration());
        return userRegistrationService.completeEmailVerifiedRegistration(
                registrationRequest, encodedPassword, requestMetadata);
    }

    public void confirmResetPasswordEmailByCode(String token, String newPassword) {
        PasswordResetTokenPayload payload = emailTokenService.consumePasswordResetToken(token);
        var user = userLookupApi.getUserByEmail(payload.email());
        userAccessControlApi.changePassword(user.id(), newPassword);
    }

    private static UserRegistrationRequest toRegistrationRequest(EmailRegistrationPayload registration) {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setFirstName(registration.firstName());
        request.setLastName(registration.lastName());
        request.setEmail(registration.email());
        return request;
    }
}
