package com.zufar.icedlatte.review.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;

/** Exercises the actual repository SQL in two independent READ COMMITTED transactions. */
@Testcontainers
class ReviewAggregateConcurrencyTest {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    private final UUID productId = UUID.randomUUID();
    private final UUID reviewId = UUID.randomUUID();

    @BeforeEach
    void schema() throws Exception {
        try (var c = connection();
                var s = c.createStatement()) {
            s.execute("DROP TABLE IF EXISTS product_reviews_likes, product_reviews, product");
            s.execute("CREATE TABLE product(id uuid primary key, average_rating numeric, reviews_count int)");
            s.execute(
                    "CREATE TABLE product_reviews(id uuid primary key, product_id uuid references product(id), rating int, likes_count int, dislikes_count int)");
            s.execute(
                    "CREATE TABLE product_reviews_likes(id uuid primary key, review_id uuid references product_reviews(id), is_like boolean)");
            s.execute("INSERT INTO product VALUES ('" + productId + "',0,0)");
            s.execute("INSERT INTO product_reviews VALUES ('" + reviewId + "','" + productId + "',1,0,0)");
            c.commit();
        }
    }

    @Test
    void simultaneousReviewsKeepTheCorrectAverage() throws Exception {
        race(
                () -> {
                    try (var c = connection()) {
                        sql(
                                c,
                                "INSERT INTO product_reviews VALUES ('" + UUID.randomUUID() + "','" + productId
                                        + "',5,0,0)");
                        query(c, ProductInfoRepository.class, "lockReviewAggregates", productId);
                        query(c, ProductInfoRepository.class, "updateAverageRating", productId);
                        c.commit();
                    }
                },
                ProductInfoRepository.class,
                "lockReviewAggregates",
                "updateAverageRating",
                productId);
        assertThat(number("SELECT average_rating FROM product")).isEqualTo(3.0);
    }

    @Test
    void simultaneousVotesAreBothCounted() throws Exception {
        race(
                () -> {
                    try (var c = connection()) {
                        query(c, ProductReviewRepository.class, "lockVotes", reviewId);
                        vote(c);
                        query(c, ProductReviewRepository.class, "updateLikesCount", reviewId);
                        c.commit();
                    }
                },
                ProductReviewRepository.class,
                "lockVotes",
                "updateLikesCount",
                reviewId);
        assertThat(number("SELECT likes_count FROM product_reviews")).isEqualTo(2.0);
    }

    private void race(CheckedTask second, Class<?> repo, String lock, String update, UUID id) throws Exception {
        try (var first = connection();
                var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            query(first, repo, lock, id);
            if (repo == ProductReviewRepository.class) vote(first);
            query(first, repo, update, id);
            var started = new CountDownLatch(1);
            var future = executor.submit(() -> {
                started.countDown();
                second.run();
                return null;
            });
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
            try (var monitor = connection()) {
                monitor.setAutoCommit(true);
                boolean blocked = false;
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                while (!blocked && System.nanoTime() < deadline) {
                    try (var s = monitor.createStatement();
                            var r = s.executeQuery(
                                    "SELECT EXISTS (SELECT 1 FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock')")) {
                        r.next();
                        blocked = r.getBoolean(1);
                    }
                    if (!blocked) Thread.sleep(10);
                }
                assertThat(blocked)
                        .as("second writer reached the contested row")
                        .isTrue();
            }
            // Release the first transaction; the second statement must see its committed writes.
            first.commit();
            future.get(15, TimeUnit.SECONDS);
        }
    }

    private Connection connection() throws Exception {
        var c = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        c.setAutoCommit(false);
        c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        return c;
    }

    private void vote(Connection c) throws Exception {
        sql(c, "INSERT INTO product_reviews_likes VALUES ('" + UUID.randomUUID() + "','" + reviewId + "',true)");
    }

    private void sql(Connection c, String sql) throws Exception {
        try (var s = c.createStatement()) {
            s.execute(sql);
        }
    }

    private void query(Connection c, Class<?> repo, String method, UUID id) throws Exception {
        String sql = repo.getMethod(method, UUID.class)
                .getAnnotation(Query.class)
                .value()
                .replaceAll(":\\w+", "?");
        try (var s = c.prepareStatement(sql)) {
            for (int i = 1; i <= s.getParameterMetaData().getParameterCount(); i++) s.setObject(i, id);
            s.execute();
        }
    }

    private double number(String sql) throws Exception {
        try (var c = connection();
                var s = c.createStatement();
                var r = s.executeQuery(sql)) {
            r.next();
            return r.getDouble(1);
        }
    }

    @FunctionalInterface
    interface CheckedTask {
        void run() throws Exception;
    }
}
