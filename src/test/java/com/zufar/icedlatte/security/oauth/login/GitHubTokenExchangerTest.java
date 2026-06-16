package com.zufar.icedlatte.security.oauth.login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.zufar.icedlatte.common.exception.UnauthorizedException;
import com.zufar.icedlatte.security.oauth.config.OAuthProvider;
import com.zufar.icedlatte.security.oauth.dto.OAuthProfile;

@DisplayName("GitHubTokenExchanger unit tests")
class GitHubTokenExchangerTest {

    @Test
    @DisplayName("buildAuthorizationUri includes GitHub OAuth parameters")
    void buildAuthorizationUriIncludesGitHubOauthParameters() {
        GitHubTokenExchanger exchanger = exchangerWithMockServer(null);

        URI uri = exchanger.buildAuthorizationUri("state-token");

        assertThat(uri.toString())
                .contains("https://github.com/login/oauth/authorize")
                .contains("client_id=client-id")
                .contains("redirect_uri=https://app.example.com/callback")
                .contains("scope=read:user%20user:email")
                .contains("state=state-token");
    }

    @Test
    @DisplayName("exchangeCode exchanges the code and loads a verified primary email")
    void exchangeCodeExchangesTheCodeAndLoadsVerifiedPrimaryEmail() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        GitHubTokenExchanger exchanger = exchangerWithMockServer(builder);

        server.expect(requestTo("https://github.com/login/oauth/access_token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Accept", MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess("""
                        {"access_token":"gho_test_token","token_type":"bearer","scope":"read:user,user:email"}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.github.com/user"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer gho_test_token"))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2026-03-10"))
                .andRespond(withSuccess("""
                        {"id":12345,"login":"octocat","name":"Ada Lovelace","email":null}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.github.com/user/emails"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer gho_test_token"))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(header("X-GitHub-Api-Version", "2026-03-10"))
                .andRespond(withSuccess("""
                        [
                          {"email":"secondary@example.com","primary":false,"verified":true},
                          {"email":"primary@example.com","primary":true,"verified":true}
                        ]
                        """, MediaType.APPLICATION_JSON));

        OAuthProfile profile = exchanger.exchangeCode("auth-code");

        assertThat(profile.providerSubject()).isEqualTo("12345");
        assertThat(profile.email()).isEqualTo("primary@example.com");
        assertThat(profile.emailVerified()).isTrue();
        assertThat(profile.firstName()).isEqualTo("Ada");
        assertThat(profile.lastName()).isEqualTo("Lovelace");
        assertThat(exchanger.provider()).isEqualTo(OAuthProvider.GITHUB);
        server.verify();
    }

    @Test
    @DisplayName("exchangeCode falls back to the login when the user name is missing")
    void exchangeCodeFallsBackToTheLoginWhenTheUserNameIsMissing() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        GitHubTokenExchanger exchanger = exchangerWithMockServer(builder);

        server.expect(requestTo("https://github.com/login/oauth/access_token"))
                .andRespond(withSuccess("""
                        {"access_token":"gho_test_token","token_type":"bearer","scope":"read:user,user:email"}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.github.com/user"))
                .andRespond(withSuccess("""
                        {"id":12345,"login":"octocat","name":" ","email":"public@example.com"}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.github.com/user/emails"))
                .andRespond(withSuccess("""
                        [{"email":"public@example.com","primary":true,"verified":true}]
                        """, MediaType.APPLICATION_JSON));

        OAuthProfile profile = exchanger.exchangeCode("auth-code");

        assertThat(profile.firstName()).isEqualTo("octocat");
        assertThat(profile.lastName()).isNull();
        server.verify();
    }

    @Test
    @DisplayName("exchangeCode rejects token responses without an access token")
    void exchangeCodeRejectsTokenResponsesWithoutAnAccessToken() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        GitHubTokenExchanger exchanger = exchangerWithMockServer(builder);

        server.expect(requestTo("https://github.com/login/oauth/access_token"))
                .andRespond(withSuccess("""
                        {"error":"bad_verification_code","error_description":"The code passed is incorrect or expired."}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> exchanger.exchangeCode("auth-code"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("GitHub authentication failed: access token exchange did not return an access token.");
        server.verify();
    }

    @Test
    @DisplayName("exchangeCode rejects user profiles without required identity fields")
    void exchangeCodeRejectsUserProfilesWithoutRequiredIdentityFields() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        GitHubTokenExchanger exchanger = exchangerWithMockServer(builder);

        server.expect(requestTo("https://github.com/login/oauth/access_token"))
                .andRespond(withSuccess("""
                        {"access_token":"gho_test_token","token_type":"bearer","scope":"read:user,user:email"}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.github.com/user"))
                .andRespond(withSuccess("""
                        {"id":null,"login":"octocat","name":"Ada Lovelace"}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> exchanger.exchangeCode("auth-code"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("GitHub authentication failed: user profile is missing required identity fields.");
        server.verify();
    }

    @Test
    @DisplayName("exchangeCode rejects accounts without a verified email")
    void exchangeCodeRejectsAccountsWithoutAVerifiedEmail() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        GitHubTokenExchanger exchanger = exchangerWithMockServer(builder);

        server.expect(requestTo("https://github.com/login/oauth/access_token"))
                .andRespond(withSuccess("""
                        {"access_token":"gho_test_token","token_type":"bearer","scope":"read:user,user:email"}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.github.com/user"))
                .andRespond(withSuccess("""
                        {"id":12345,"login":"octocat","name":"Ada Lovelace","email":null}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.github.com/user/emails"))
                .andRespond(withSuccess("""
                        [{"email":"user@example.com","primary":true,"verified":false}]
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> exchanger.exchangeCode("auth-code"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("GitHub authentication failed: account has no verified email address.");
        server.verify();
    }

    private static GitHubTokenExchanger exchangerWithMockServer(RestClient.Builder builder) {
        RestClient.Builder effectiveBuilder = builder == null ? RestClient.builder() : builder;
        return new GitHubTokenExchanger(
                "client-id",
                "client-secret",
                "https://app.example.com/callback",
                "read:user user:email",
                "2026-03-10",
                effectiveBuilder.build());
    }
}
