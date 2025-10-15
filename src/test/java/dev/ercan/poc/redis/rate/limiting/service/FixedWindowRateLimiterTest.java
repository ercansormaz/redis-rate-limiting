package dev.ercan.poc.redis.rate.limiting.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisCommands;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FixedWindowRateLimiterTest {

  @Autowired
  private FixedWindowRateLimiter fixedWindowRateLimiter;

  @Autowired
  private RedisCommands redisCommands;

  private final String id = "FixedWindowRateLimiterTest";
  private final String key = "127.0.0.1";
  private final long limit = 5;
  private final Duration windowDuration = Duration.ofSeconds(2);

  @BeforeEach
  @AfterEach
  void setup() {
    redisCommands.del(("fixed_window:" + id + ":" + key).getBytes());
  }

  @Test
  void shouldAllowRequestsWithinLimit() {
    for (int i = 0; i < limit; i++) {
      boolean allowed = fixedWindowRateLimiter.tryConsume(id, key, limit, windowDuration);
      assertTrue(allowed, "Requests below the limit must be accepted during the window period.");
    }
  }

  @Test
  void shouldRejectRequestsOverLimitInSameWindow() {
    for (int i = 0; i < limit; i++) {
      fixedWindowRateLimiter.tryConsume(id, key, limit, windowDuration);
    }
    boolean allowed = fixedWindowRateLimiter.tryConsume(id, key, limit, windowDuration);
    assertFalse(allowed, "Requests should be rejected when the limit is exceeded.");
  }

  @Test
  void shouldResetWindowAndAcceptRequestsAgain() throws InterruptedException {
    for (int i = 0; i < limit; i++) {
      fixedWindowRateLimiter.tryConsume(id, key, limit, windowDuration);
    }
    assertFalse(fixedWindowRateLimiter.tryConsume(id, key, limit, windowDuration));
    // Wait until window expire
    Thread.sleep(windowDuration.toMillis() + 200);
    boolean allowed = fixedWindowRateLimiter.tryConsume(id, key, limit, windowDuration);
    assertTrue(allowed, "Request should be accepted when window expire.");
  }

  @Test
  void shouldHandleMultipleKeysSeparately() {
    String anotherKey = "127.0.0.2";
    for (int i = 0; i < limit; i++) {
      fixedWindowRateLimiter.tryConsume(id, key, limit, windowDuration);
    }
    for (int i = 0; i < limit; i++) {
      boolean allowed = fixedWindowRateLimiter.tryConsume(id, anotherKey, limit, windowDuration);
      assertTrue(allowed, "There should be a separate window for each key.");
    }
  }

  @Test
  void shouldHandleMultipleIdsSeparately() {
    String anotherId = "AnotherFixedWindowRateLimiterTest";
    for (int i = 0; i < limit; i++) {
      fixedWindowRateLimiter.tryConsume(id, key, limit, windowDuration);
    }
    for (int i = 0; i < limit; i++) {
      boolean allowed = fixedWindowRateLimiter.tryConsume(anotherId, key, limit, windowDuration);
      assertTrue(allowed, "There should be a separate window for each id.");
    }
  }

  @Test
  void shouldRejectRequestsForZeroOrNegativeLimit() {
    boolean allowed = fixedWindowRateLimiter.tryConsume(id, key, 0, windowDuration);
    assertFalse(allowed, "Request should be rejected when limit given as zero.");

    allowed = fixedWindowRateLimiter.tryConsume(id, key, -5, windowDuration);
    assertFalse(allowed, "Request should be rejected when limit given as negative.");
  }

  @Test
  void shouldBeThreadSafeUnderConcurrency() throws InterruptedException {
    int threads = 20;
    ExecutorService service = Executors.newFixedThreadPool(threads);
    CountDownLatch latch = new CountDownLatch(threads);
    AtomicInteger acceptedCount = new AtomicInteger(0);

    for (int i = 0; i < threads; i++) {
      service.submit(() -> {
        if (fixedWindowRateLimiter.tryConsume(id, key, limit, windowDuration)) {
          acceptedCount.incrementAndGet();
        }
        latch.countDown();
      });
    }
    latch.await();
    service.shutdown();

    assertEquals(limit, acceptedCount.get(),
        "The window limit should not be exceeded for parallel requests.");
  }
}