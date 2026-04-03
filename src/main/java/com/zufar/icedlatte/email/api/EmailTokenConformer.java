package com.zufar.icedlatte.email.api;

import com.zufar.icedlatte.email.api.token.TokenManager;
import com.zufar.icedlatte.email.api.token.TokenPurpose;
import com.zufar.icedlatte.openapi.dto.ConfirmEmailRequest;
import com.zufar.icedlatte.openapi.dto.UserAuthenticationResponse;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import com.zufar.icedlatte.security.api.UserRegistrationService;
import com.zufar.icedlatte.user.api.ChangeUserPasswordOperationPerformer;
import com.zufar.icedlatte.user.api.SingleUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailTokenConformer {

    private final UserRegistrationService userRegistrationService;
    private final TokenManager tokenManager;
    private final SingleUserProvider singleUserProvider;
    private final ChangeUserPasswordOperationPerformer changeUserPasswordOperationPerformer;

    public UserAuthenticationResponse confirmEmailByCode(final ConfirmEmailRequest confirmEmailRequest, final HttpServletRequest httpRequest) {
        UserRegistrationRequest userRegistrationRequest =
                tokenManager.validateToken(confirmEmailRequest, TokenPurpose.EMAIL_VERIFICATION);
        return userRegistrationService.register(userRegistrationRequest, httpRequest);
    }

    public void confirmResetPasswordEmailByCode(final ConfirmEmailRequest confirmEmailRequest, final String newPassword) {
        UserRegistrationRequest request =
                tokenManager.validateToken(confirmEmailRequest, TokenPurpose.PASSWORD_RESET);
        var userEntity = singleUserProvider.getUserEntityByEmail(request.getEmail());
        changeUserPasswordOperationPerformer.changeUserPassword(userEntity.getId(), newPassword);
    }
}
