package com.zufar.icedlatte.test.config;

import com.zufar.icedlatte.user.entity.UserEntity;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.restassured.specification.RequestSpecification;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;


@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractE2ETest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13.11-bullseye");

    @LocalServerPort
    protected Integer port;

    protected static String secretKey = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    protected static Long expiration = 1800000L;

    protected static String jwtToken = "";

    @DynamicPropertySource
    static void dataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("DATASOURCE_URL", postgres::getJdbcUrl);
    }

    protected static RequestSpecification specification;

    protected static void generateJwtToken(){
        UserEntity userDetails = new UserEntity();
        userDetails.setEmail("john@example.com");
        userDetails.setPassword("password123");
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        Key secretHmac = Keys.hmacShaKeyFor(keyBytes);
        jwtToken = Jwts.builder()
                .setClaims(new HashMap<>())
                .setSubject(userDetails.getEmail())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(secretHmac, SignatureAlgorithm.HS256)
                .compact();
    }
}
