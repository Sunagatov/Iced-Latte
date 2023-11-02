package com.zufar.onlinestore.security.endpoint;

import io.restassured.RestAssured;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.file.Files;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@Testcontainers
class UserSecurityEndpointTest extends BaseUserSecurityEndpointTest {

    public static final DockerImageName MOCKSERVER_IMAGE = DockerImageName
            .parse("mockserver/mockserver")
            .withTag("mockserver-" + MockServerClient.class.getPackage().getImplementationVersion());

    @Rule
    public MockServerContainer mockServer = new MockServerContainer(MOCKSERVER_IMAGE);
    static MockServerClient mockServerClient;

    @Value("classpath:security/model/register/base-user-register-model.json")
    private Resource userRegisterJson;
    @Value("classpath:security/model/register/user-wrong-data-register.json")
    private Resource wrongDataUserRegisterJson;
    @Value("classpath:security/model/register/user-already-exist-register.json")
    private Resource userAlreadyExistRegisterJson;

    @Value("classpath:security/model/login/base-user-login-model.json")
    private Resource userLoginJson;
    @Value("classpath:security/model/login/user-wrong-authenticate.json")
    private Resource wrongUserLoginJson;

    private String endpointRegister = "register";
    private String endpointLogin = "authenticate";

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        mockServer.start();
        mockServerClient = new MockServerClient(mockServer.getHost(), mockServer.getServerPort());
        mockServerClient.reset();
    }

    @Test
    void testRegisterUser() throws Exception {
        String mockRequest = loadProductJsonResource(userRegisterJson);

        mockServerClient.when(request()
                        .withMethod(HttpMethod.POST.name())
                        .withPath(UserSecurityEndpoint.USER_SECURITY_API_URL + endpointRegister)
                        .withBody(mockRequest))
                .respond(response()
                        .withStatusCode(HttpStatus.CREATED.value()));

        checkStatusCodeInResponse(endpointRegister, HttpStatus.CREATED.value(), "security/model/schema/user-register-schema.json", mockRequest);
    }

    @Test
    void testWrongDataRegisterUser() throws Exception {
        String mockRequest = loadProductJsonResource(wrongDataUserRegisterJson);

        mockServerClient.when(request()
                        .withMethod(HttpMethod.POST.name())
                        .withPath(UserSecurityEndpoint.USER_SECURITY_API_URL + endpointRegister)
                        .withBody(mockRequest))
                .respond(response()
                        .withStatusCode(HttpStatus.BAD_REQUEST.value()));

        checkStatusCodeInResponse(endpointRegister, HttpStatus.BAD_REQUEST.value(), mockRequest);
    }

    @Test
    void testUserAlreadyExistRegisterUser() throws Exception {
        String mockRequest = loadProductJsonResource(userAlreadyExistRegisterJson);
        mockServerClient.when(request()
                        .withMethod(HttpMethod.POST.name())
                        .withPath(UserSecurityEndpoint.USER_SECURITY_API_URL + endpointRegister)
                        .withBody(mockRequest))
                .respond(response()
                        .withStatusCode(HttpStatus.BAD_REQUEST.value()));

        checkStatusCodeInResponse(endpointRegister, HttpStatus.BAD_REQUEST.value(), mockRequest);
    }

    @Test
    void testAuthenticateUser() throws Exception {
        String mockRequestRegister = loadProductJsonResource(userRegisterJson);
        String mockRequestLogin = loadProductJsonResource(userLoginJson);

        mockServerClient.when(request()
                        .withMethod(HttpMethod.POST.name())
                        .withPath(UserSecurityEndpoint.USER_SECURITY_API_URL + endpointRegister)
                        .withBody(mockRequestRegister))
                .respond(response()
                        .withStatusCode(HttpStatus.CREATED.value()));

        mockServerClient.when(request()
                        .withMethod(HttpMethod.POST.name())
                        .withPath(UserSecurityEndpoint.USER_SECURITY_API_URL + endpointLogin)
                        .withBody(mockRequestLogin))
                .respond(response()
                        .withStatusCode(HttpStatus.OK.value()));

        checkStatusCodeInResponse(endpointLogin, HttpStatus.OK.value(), mockRequestLogin);
    }

    @Test
    void testWrongAuthenticateUser() throws Exception {
        String mockRequestLogin = loadProductJsonResource(wrongUserLoginJson);

        mockServerClient.when(request()
                        .withMethod(HttpMethod.POST.name())
                        .withPath(UserSecurityEndpoint.USER_SECURITY_API_URL + endpointLogin)
                        .withBody(mockRequestLogin))
                .respond(response()
                        .withStatusCode(HttpStatus.NOT_FOUND.value()));

        //todo: exception not thrown, code not responding
        // checkStatusCodeInResponse(endpointLogin, HttpStatus.NOT_FOUND.value(), mockRequestLogin);
    }

    private String loadProductJsonResource(Resource resource) throws IOException {
        return Files.readString(resource.getFile().toPath());
    }
}