package com.zufar.icedlatte.security.oauth.login;

import java.net.URI;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zufar.icedlatte.common.exception.UnauthorizedException;
import com.zufar.icedlatte.security.oauth.config.GitHubOAuthProperties;
import com.zufar.icedlatte.security.oauth.config.OAuthProvider;
import com.zufar.icedlatte.security.oauth.dto.OAuthProfile;

@Service
@ConditionalOnProperty(name = "github.enabled", havingValue = "true")
public class GitHubTokenExchanger implements OAuthProviderClient {

    private static final String AUTHORIZE_URL = "https://github.com/login/oauth/authorize";
    private static final String ACCESS_TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String USER_URL = "https://api.github.com/user";
    private static final String USER_EMAILS_URL = "https://api.github.com/user/emails";
    private static final String GITHUB_JSON_MEDIA_TYPE = "application/vnd.github+json";
    private static final String MISSING_ACCESS_TOKEN_MESSAGE =
            "GitHub authentication failed: access token exchange did not return an access token.";
    private static final String MISSING_IDENTITY_FIELDS_MESSAGE =
            "GitHub authentication failed: user profile is missing required identity fields.";
    private static final String MISSING_VERIFIED_EMAIL_MESSAGE =
            "GitHub authentication failed: account has no verified email address.";

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String scope;
    private final String apiVersion;
    private final RestClient restClient;

    @Autowired
    public GitHubTokenExchanger(GitHubOAuthProperties properties) {
        GitHubOAuthProperties.Timeout timeout = properties.timeout();
        this(
                properties.clientId(),
                properties.clientSecret(),
                properties.redirectUri(),
                properties.scope(),
                properties.apiVersion(),
                restClient(timeout.connectTimeout(), timeout.readTimeout()));
    }

    GitHubTokenExchanger(
            String clientId,
            String clientSecret,
            String redirectUri,
            String scope,
            String apiVersion,
            RestClient restClient) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.scope = scope;
        this.apiVersion = apiVersion;
        this.restClient = restClient;
    }

    private static RestClient restClient(Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.GITHUB;
    }

    @Override
    public URI buildAuthorizationUri(String state) {
        return UriComponentsBuilder.fromUriString(AUTHORIZE_URL)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", scope)
                .queryParam("state", state)
                .build()
                .toUri();
    }

    @Override
    public OAuthProfile exchangeCode(String authorizationCode) {
        GitHubTokenResponse tokenResponse = exchangeForToken(authorizationCode);
        if (tokenResponse == null
                || tokenResponse.accessToken() == null
                || tokenResponse.accessToken().isBlank()) {
            throw new UnauthorizedException(MISSING_ACCESS_TOKEN_MESSAGE);
        }

        GitHubUserProfile userProfile = loadUserProfile(tokenResponse.accessToken());
        if (userProfile == null
                || userProfile.id() == null
                || userProfile.login() == null
                || userProfile.login().isBlank()) {
            throw new UnauthorizedException(MISSING_IDENTITY_FIELDS_MESSAGE);
        }

        GitHubEmailAddress email = resolveEmail(tokenResponse.accessToken())
                .orElseThrow(() -> new UnauthorizedException(MISSING_VERIFIED_EMAIL_MESSAGE));
        NameParts nameParts = splitName(userProfile.name(), userProfile.login());

        return new OAuthProfile(
                String.valueOf(userProfile.id()),
                email.email(),
                email.verified(),
                nameParts.firstName(),
                nameParts.lastName());
    }

    private GitHubTokenResponse exchangeForToken(String authorizationCode) {
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", authorizationCode);
        form.add("redirect_uri", redirectUri);

        return restClient
                .post()
                .uri(ACCESS_TOKEN_URL)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(GitHubTokenResponse.class);
    }

    private GitHubUserProfile loadUserProfile(String accessToken) {
        return restClient
                .get()
                .uri(USER_URL)
                .headers(headers -> applyGitHubHeaders(headers, accessToken))
                .retrieve()
                .body(GitHubUserProfile.class);
    }

    private Optional<GitHubEmailAddress> resolveEmail(String accessToken) {
        return Optional.ofNullable(restClient
                        .get()
                        .uri(USER_EMAILS_URL)
                        .headers(headers -> applyGitHubHeaders(headers, accessToken))
                        .retrieve()
                        .body(GitHubEmailAddress[].class))
                .map(List::of)
                .orElse(List.of())
                .stream()
                .filter(GitHubEmailAddress::verified)
                .filter(email -> email.email() != null && !email.email().isBlank())
                .max(Comparator.comparing(GitHubEmailAddress::primary));
    }

    private void applyGitHubHeaders(HttpHeaders headers, String accessToken) {
        headers.setBearerAuth(accessToken);
        headers.set(HttpHeaders.ACCEPT, GITHUB_JSON_MEDIA_TYPE);
        headers.set("X-GitHub-Api-Version", apiVersion);
    }

    private static NameParts splitName(String fullName, String fallbackLogin) {
        if (fullName == null || fullName.isBlank()) {
            return new NameParts(fallbackLogin, null);
        }
        String normalized = fullName.trim();
        int separator = normalized.indexOf(' ');
        if (separator < 0) {
            return new NameParts(normalized, null);
        }

        String firstName = normalized.substring(0, separator).trim();
        firstName = firstName.isBlank() ? fallbackLogin : firstName;

        String lastName = normalized.substring(separator + 1).trim();
        lastName = lastName.isBlank() ? null : lastName;

        return new NameParts(firstName, lastName);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GitHubTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            String scope,
            String error,
            @JsonProperty("error_description") String errorDescription) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GitHubUserProfile(Long id, String login, String email, String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GitHubEmailAddress(String email, boolean primary, boolean verified) {}

    private record NameParts(String firstName, String lastName) {}
}
