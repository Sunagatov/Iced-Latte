package com.zufar.icedlatte.auth.endpoint;

import com.zufar.icedlatte.auth.api.OAuthFlowService;
import com.zufar.icedlatte.auth.api.OAuthProvider;
import com.zufar.icedlatte.security.configuration.AuthPaths;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping(AuthPaths.ROOT)
public class AuthEndpoint {

    private final OAuthFlowService oAuthFlowService;

    public AuthEndpoint(OAuthFlowService oAuthFlowService) {
        this.oAuthFlowService = oAuthFlowService;
    }

    @GetMapping("/google")
    public ResponseEntity<?> initiateGoogleAuth(@RequestParam(required = false) String redirectUrl) {
        return initiateOAuth("google", redirectUrl);
    }

    @GetMapping("/oauth/{provider}")
    public ResponseEntity<?> initiateOAuth(@PathVariable String provider,
                                           @RequestParam(required = false) String redirectUrl) {
        OAuthProvider oAuthProvider = parseProvider(provider);
        return oAuthFlowService.initiate(oAuthProvider, redirectUrl)
                .map(authUri -> ResponseEntity.status(HttpStatus.FOUND).location(authUri).build())
                .orElseGet(() -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("message", "OAuth provider is not available.", "status", 503)));
    }

    @GetMapping("/google/callback")
    public void googleCallback(@RequestParam(required = false) String code,
                               @RequestParam(required = false) String state,
                               HttpServletRequest request,
                               HttpServletResponse response) throws IOException {
        oAuthCallback("google", code, state, request, response);
    }

    @GetMapping("/oauth/{provider}/callback")
    public void oAuthCallback(@PathVariable String provider,
                              @RequestParam(required = false) String code,
                              @RequestParam(required = false) String state,
                              HttpServletRequest request,
                              HttpServletResponse response) throws IOException {
        OAuthProvider oAuthProvider = parseProvider(provider);
        response.sendRedirect(oAuthFlowService.completeCallback(oAuthProvider, code, state, request).toString());
    }

    private OAuthProvider parseProvider(String provider) {
        return OAuthProvider.fromId(provider)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "OAuth provider is not supported."));
    }
}
