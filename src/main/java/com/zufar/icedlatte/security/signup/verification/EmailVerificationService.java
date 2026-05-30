package com.zufar.icedlatte.security.signup.verification;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;

import com.zufar.icedlatte.common.util.EmailNormalizer;
import com.zufar.icedlatte.openapi.dto.ConfirmEmailRequest;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.security.email.sender.AuthTokenEmailSender;
import com.zufar.icedlatte.security.session.dto.TokenPurpose;
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
        String token = emailTokenService.generate(request, TokenPurpose.EMAIL_VERIFICATION);
        emailConfirmation.sendTemporaryCode(EmailNormalizer.normalize(request.getEmail()), token);
    }

    public void sendPasswordResetCode(String email) {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail(EmailNormalizer.normalize(email));
        String token = emailTokenService.generate(request, TokenPurpose.PASSWORD_RESET);
        emailConfirmation.sendTemporaryCode(request.getEmail(), token);
    }

    public AuthenticationTokens confirmEmailByCode(
            ConfirmEmailRequest confirmEmailRequest, HttpServletRequest httpRequest) {
        EmailTokenEntry entry = emailTokenService.consume(confirmEmailRequest, TokenPurpose.EMAIL_VERIFICATION);
        EmailRegistrationPayload registration = entry.registration();
        if (registration == null) {
            throw new IllegalStateException("Email verification token is missing registration payload");
        }
        String encodedPassword = entry.encodedPassword();
        if (encodedPassword == null || encodedPassword.isBlank()) {
            throw new IllegalStateException("Email verification token is missing encoded password");
        }
        UserRegistrationRequest registrationRequest = toRegistrationRequest(registration);
        return userRegistrationService.completeEmailVerifiedRegistration(
                registrationRequest, encodedPassword, httpRequest);
    }

    public void confirmResetPasswordEmailByCode(ConfirmEmailRequest confirmEmailRequest, String newPassword) {
        EmailTokenEntry entry = emailTokenService.consume(confirmEmailRequest, TokenPurpose.PASSWORD_RESET);
        var user = userLookupApi.getUserByEmail(entry.email());
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
