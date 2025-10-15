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
class SlidingWindowLogRateLimiterTest {

  @Autowired
  private SlidingWindowLogRateLimiter slidingWindowLogRateLimiter;

  @Autowired
  private RedisCommands redisCommands;

  private final String id = "SlidingWindowLogRateLimiterTest";
  private final String key = "127.0.0.1";
  private final long limit = 3;
  private final Duration windowDuration = Duration.ofSeconds(10);

  @BeforeEach
  @AfterEach
  void setup() {
    redisCommands.del(("sliding_window_log:" + id + ":" + key).getBytes());
  }

  @Test
  void shouldAllowRequestsWithinLimit() {
    for (int i = 0; i < limit; i++) {
      boolean allowed = slidingWindowLogRateLimiter.tryConsume(id, key, limit, windowDuration);
      assertTrue(allowed, "Requests below the limit must be accepted during the window period.");
    }
  }

  @Test
  void shouldRejectRequestsOverLimitInSameWindow() {
    for (int i = 0; i < limit; i++) {
      slidingWindowLogRateLimiter.tryConsume(id, key, limit, windowDuration);
    }
    boolean allowed = slidingWindowLogRateLimiter.tryConsume(id, key, limit, windowDuration);
    assertFalse(allowed, "Requests should be rejected when the limit is exceeded.");
  }

  @Test
  void shouldResetWindowAndAcceptRequestsAgain() throws InterruptedException {
    for (int i = 0; i < limit; i++) {
      slidingWindowLogRateLimiter.tryConsume(id, key, limit, windowDuration);
    }
    assertFalse(slidingWindowLogRateLimiter.tryConsume(id, key, limit, windowDuration));
    // Wait until window expire
    Thread.sleep(windowDuration.toMillis() + 200);
    boolean allowed = slidingWindowLogRateLimiter.tryConsume(id, key, limit, windowDuration);
    assertTrue(allowed, "Request should be accepted when window expire.");
  }

  @Test
  void shouldHandleMultipleKeysSeparately() {
    String anotherKey = "127.0.0.2";
    for (int i = 0; i < limit; i++) {
      slidingWindowLogRateLimiter.tryConsume(id, key, limit, windowDuration);
    }
    for (int i = 0; i < limit; i++) {
      boolean allowed = slidingWindowLogRateLimiter.tryConsume(id, anotherKey, limit, windowDuration);
      assertTrue(allowed, "There should be a separate window for each key.");
    }
  }

  @Test
  void shouldHandleMultipleIdsSeparately() {
    String anotherId = "AnotherSlidingWindowLogRateLimiterTest";
    for (int i = 0; i < limit; i++) {
      slidingWindowLogRateLimiter.tryConsume(id, key, limit, windowDuration);
    }
    for (int i = 0; i < limit; i++) {
      boolean allowed = slidingWindowLogRateLimiter.tryConsume(anotherId, key, limit, windowDuration);
      assertTrue(allowed, "There should be a separate window for each id.");
    }
  }

  @Test
  void shouldRejectRequestsForZeroOrNegativeLimit() {
    boolean allowed = slidingWindowLogRateLimiter.tryConsume(id, key, 0, windowDuration);
    assertFalse(allowed, "Request should be rejected when limit given as zero.");

    allowed = slidingWindowLogRateLimiter.tryConsume(id, key, -5, windowDuration);
    assertFalse(allowed, "Request should be rejected when limit given as negative.");
  }

  @Test
  void shouldLogOnlySuccessfulRequests() {
    for (int i = 0; i < limit + 2; i++) {
      slidingWindowLogRateLimiter.tryConsume(id, key, limit, windowDuration);
    }

    Long logSize = redisCommands.zCard(("sliding_window_log:" + id + ":" + key).getBytes());
    assertEquals(limit, logSize, "Only successful requests should be logged.");
  }

  @Test
  void shouldRemoveOldRequestsFromLog() throws InterruptedException {
    for (int i = 0; i < limit; i++) {
      slidingWindowLogRateLimiter.tryConsume(id, key, limit, windowDuration);
    }
    Thread.sleep(windowDuration.toMillis() + 200);
    slidingWindowLogRateLimiter.tryConsume(id, key, limit, windowDuration);
    Long logSize = redisCommands.zCard(("sliding_window_log:" + id + ":" + key).getBytes());
    assertEquals(1, logSize, "Old requests should be cleaned up when they go out of the window.");
  }

  @Test
  void shouldBeThreadSafeUnderConcurrency() throws InterruptedException {
    int threads = 20;
    ExecutorService service = Executors.newFixedThreadPool(threads);
    CountDownLatch latch = new CountDownLatch(threads);
    AtomicInteger acceptedCount = new AtomicInteger(0);

    for (int i = 0; i < threads; i++) {
      service.submit(() -> {
        if (slidingWindowLogRateLimiter.tryConsume(id, key, limit, windowDuration)) {
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
