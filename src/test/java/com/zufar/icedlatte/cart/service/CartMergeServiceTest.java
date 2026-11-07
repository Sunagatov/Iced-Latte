package com.zufar.icedlatte.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.zufar.icedlatte.cart.api.dto.AddCartItemRequest;
import com.zufar.icedlatte.cart.exception.InvalidCartItemRequestException;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;

@Testcontainers
class CartMergeServiceTest {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:13.11-bullseye");

    private final UUID userId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private final UUID key = UUID.randomUUID();
    private JdbcTemplate jdbc;
    private ShoppingCartService carts;
    private CartMergeService merges;
    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void setUp() throws Exception {
        context = new AnnotationConfigApplicationContext(Config.class);
        jdbc = context.getBean(JdbcTemplate.class);
        carts = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(
                context.getBean(ShoppingCartService.class));
        merges = context.getBean(CartMergeService.class);
        reset(carts);
        jdbc.execute("DROP TABLE IF EXISTS cart_merge_receipt");
        jdbc.execute("CREATE TABLE IF NOT EXISTS user_details (id UUID PRIMARY KEY)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS test_cart (user_id UUID PRIMARY KEY, quantity INT NOT NULL)");
        jdbc.execute(new ClassPathResource("db/changelog/version-2.0/07.11.2026.part1.create-cart-merge-receipt.sql")
                .getContentAsString(StandardCharsets.UTF_8));
        addUser(userId);
        when(carts.addItemsToCart(any(), any())).thenAnswer(invocation -> {
            UUID user = invocation.getArgument(0);
            Set<AddCartItemRequest> items = invocation.getArgument(1);
            int amount =
                    items.stream().mapToInt(AddCartItemRequest::productQuantity).sum();
            jdbc.update("UPDATE test_cart SET quantity = quantity + ? WHERE user_id = ?", amount, user);
            return currentCart(user);
        });
        when(carts.getByUserId(any())).thenAnswer(invocation -> currentCart(invocation.getArgument(0)));
    }

    @org.junit.jupiter.api.AfterEach
    void close() {
        context.close();
    }

    @Test
    void lostReplyCanBeRetriedWithoutAddingAgainAndReturnsCurrentCart() {
        merges.merge(userId, key, items(2));
        // The first response was lost; another edit then changed the cart.
        jdbc.update("UPDATE test_cart SET quantity = 5 WHERE user_id = ?", userId);
        assertThat(merges.merge(userId, key, items(2)).getProductsQuantity()).isEqualTo(5);
        verify(carts, times(1)).addItemsToCart(userId, items(2));
    }

    @Test
    void changedPayloadIsRejectedWithoutAddingAgain() {
        merges.merge(userId, key, items(2));
        assertThatThrownBy(() -> merges.merge(userId, key, items(3)))
                .isInstanceOf(InvalidCartItemRequestException.class);
        assertThat(currentCart(userId).getProductsQuantity()).isEqualTo(2);
    }

    @Test
    void sameKeyIsScopedToItsAccount() {
        UUID other = UUID.randomUUID();
        addUser(other);
        merges.merge(userId, key, items(2));
        assertThat(merges.merge(other, key, items(3)).getProductsQuantity()).isEqualTo(3);
    }

    @Test
    void failedCartSaveRollsBackReceiptAndCanBeRetried() {
        AtomicBoolean fail = new AtomicBoolean(true);
        doAnswer(invocation -> {
                    jdbc.update("UPDATE test_cart SET quantity = quantity + 2 WHERE user_id = ?", userId);
                    if (fail.getAndSet(false)) throw new IllegalStateException("save failed");
                    return currentCart(userId);
                })
                .when(carts)
                .addItemsToCart(userId, items(2));

        assertThatThrownBy(() -> merges.merge(userId, key, items(2))).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM cart_merge_receipt", Long.class))
                .isZero();
        assertThat(currentCart(userId).getProductsQuantity()).isZero();
        assertThat(merges.merge(userId, key, items(2)).getProductsQuantity()).isEqualTo(2);
    }

    @Test
    void concurrentRetriesApplyOnlyOnce() throws Exception {
        CountDownLatch saving = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> {
                    saving.countDown();
                    assertThat(release.await(10, TimeUnit.SECONDS)).isTrue();
                    jdbc.update("UPDATE test_cart SET quantity = quantity + 2 WHERE user_id = ?", userId);
                    return currentCart(userId);
                })
                .when(carts)
                .addItemsToCart(userId, items(2));

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> merges.merge(userId, key, items(2)));
            assertThat(saving.await(10, TimeUnit.SECONDS)).isTrue();
            CountDownLatch retryStarted = new CountDownLatch(1);
            var retry = executor.submit(() -> {
                retryStarted.countDown();
                return merges.merge(userId, key, items(2));
            });
            assertThat(retryStarted.await(10, TimeUnit.SECONDS)).isTrue();
            release.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS).getProductsQuantity()).isEqualTo(2);
            assertThat(retry.get(10, TimeUnit.SECONDS).getProductsQuantity()).isEqualTo(2);
        } finally {
            release.countDown();
        }
        verify(carts, times(1)).addItemsToCart(userId, items(2));
    }

    private void addUser(UUID user) {
        jdbc.update("INSERT INTO user_details (id) VALUES (?)", user);
        jdbc.update("INSERT INTO test_cart VALUES (?, 0)", user);
    }

    private ShoppingCartDto currentCart(UUID user) {
        return new ShoppingCartDto()
                .userId(user)
                .productsQuantity(
                        jdbc.queryForObject("SELECT quantity FROM test_cart WHERE user_id = ?", Integer.class, user));
    }

    private Set<AddCartItemRequest> items(int quantity) {
        return Set.of(new AddCartItemRequest(productId, quantity));
    }

    @Configuration
    @EnableTransactionManagement(proxyTargetClass = true)
    static class Config {
        @Bean
        DataSource dataSource() {
            return new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        ShoppingCartService shoppingCartService() {
            return mock(ShoppingCartService.class);
        }

        @Bean
        CartMergeService cartMergeService(JdbcTemplate jdbc, ShoppingCartService carts) {
            return new CartMergeService(jdbc, carts);
        }
    }
}
