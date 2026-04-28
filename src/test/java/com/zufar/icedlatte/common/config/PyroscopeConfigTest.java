package com.zufar.icedlatte.common.config;

import io.pyroscope.javaagent.PyroscopeAgent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

@DisplayName("PyroscopeConfig unit tests")
class PyroscopeConfigTest {

    @Test
    @DisplayName("does nothing when no server address is configured")
    void doesNothingWhenNoServerAddressIsConfigured() {
        PyroscopeConfig config = config("", "", "");

        try (MockedStatic<PyroscopeAgent> mocked = mockStatic(PyroscopeAgent.class)) {
            config.startPyroscope();

            mocked.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("starts Pyroscope with the configured application and auth settings")
    void startsPyroscopeWithConfiguredApplicationAndAuthSettings() {
        PyroscopeConfig config = config(
                "https://pyroscope.example.com",
                "user",
                "password");

        try (MockedStatic<PyroscopeAgent> mocked = mockStatic(PyroscopeAgent.class)) {
            config.startPyroscope();

            mocked.verify(() -> PyroscopeAgent.start(any(PyroscopeAgent.Options.class)));
        }
    }

    private static PyroscopeConfig config(String serverAddress,
                                          String basicAuthUser,
                                          String basicAuthPassword) {
        PyroscopeConfig config = new PyroscopeConfig();
        ReflectionTestUtils.setField(config, "serverAddress", serverAddress);
        ReflectionTestUtils.setField(config, "applicationName", "iced-latte-backend");
        ReflectionTestUtils.setField(config, "basicAuthUser", basicAuthUser);
        ReflectionTestUtils.setField(config, "basicAuthPassword", basicAuthPassword);
        return config;
    }
}
