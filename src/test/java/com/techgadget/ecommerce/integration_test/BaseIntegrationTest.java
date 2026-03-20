package com.techgadget.ecommerce.integration_test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Creates a base for integration testing (container, dependency, etc)
 *
 * NOTES:
 * Make sure DOCKER is running in your local machine before doing integration test!
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public abstract class BaseIntegrationTest {

    // Create postgres container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("techgadget_test")
            .withUsername("test")
            .withPassword("test")
            .withExposedPorts(5432);

    // Create redis container (Redis:7-alpine)
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    // Start all container
    static {
        postgres.start();
        redis.start();
    }

    /**
     * Executed before ApplicationContext created.
     * Must be static
     */
    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl());
        registry.add("spring.datasource.username", () -> postgres.getUsername());
        registry.add("spring.datasource.password", () -> postgres.getPassword());
        registry.add("spring.data.redis.host", () -> redis.getHost());
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.lettuce.pool.enabled", () -> true);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * Flush all data in Redis
     */
    @BeforeEach
    void cleanUpRedis() {

        Assertions.assertNotNull(stringRedisTemplate.getConnectionFactory());
        stringRedisTemplate.getConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushDb();
    }

}
