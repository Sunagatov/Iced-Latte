package com.zufar.onlinestore.security.endpoint;

import com.zufar.onlinestore.payment.converter.StripePaymentMethodConverter;
import com.zufar.onlinestore.user.entity.UserEntity;
import com.zufar.onlinestore.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
//todo check, why it's don't work from docker
class UserSecurityEndpointTest {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();
    private String name = "name";
    private String lastName = "lastName";
    private String email = "al1@gmail.com";
    private String password = "password123!";

    private String registerJson;
    private String loginJson;
    @Container
    public static PostgreSQLContainer<?> POSTGRESQL_CONTAINER =
            new PostgreSQLContainer<>("postgres:13.11-bullseye");
    @Autowired
    private StripePaymentMethodConverter stripePaymentMethodConverter;


    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {
        POSTGRESQL_CONTAINER.start();

        registry.add("database.url", POSTGRESQL_CONTAINER::getJdbcUrl);
        registry.add("database.username", POSTGRESQL_CONTAINER::getUsername);
        registry.add("database.password", POSTGRESQL_CONTAINER::getPassword);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    @BeforeEach
    void setUp() {
        registerJson = String.format("""
            {
              "firstName": "%s",
              "lastName": "%s",
              "email": "%s",
              "password": "%s"
            }
            """, name, lastName, email, password);

        loginJson = String.format("""
            {
              "email": "%s",
              "password": "%s"
            }
            """, email, password);
    }

    @Test
    @Transactional
    void register() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(registerJson))
                .andExpect(status().isCreated());

        UserEntity user = userRepository.findByEmail(email).get();
        assertEquals(name, user.getFirstName());
        assertEquals(lastName, user.getLastName());
        assertTrue(user.getId() != null);
    }

    @Test
    @Transactional
    void authenticate() throws Exception {
        //todo think about saving test user,
        // i need to encode password and set up entity values
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(registerJson))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType("application/json")
                        .content(loginJson))
                .andExpect(status().isOk());
    }

    /*
    @Test
    @Transactional
    void logout() throws Exception {
        var data = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(registerJson))
                .andExpect(status().isCreated())
                .andReturn();

        String tokenStr = data.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(tokenStr);
        String token = jsonNode.get("token").asText();

        // todo cant open logout endpoint
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

     */
}