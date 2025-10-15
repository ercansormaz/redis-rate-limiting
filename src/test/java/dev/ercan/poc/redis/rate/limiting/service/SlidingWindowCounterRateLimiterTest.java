package dev.ercan.poc.redis.rate.limiting.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
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
class SlidingWindowCounterRateLimiterTest {

  @Autowired
  private SlidingWindowCounterRateLimiter slidingWindowCounterRateLimiter;

  @Autowired
  private RedisCommands redisCommands;

  private final String id = "SlidingWindowCounterRateLimiterTest";
  private final String key = "127.0.0.1";
  private final long limit = 5;
  private final Duration windowDuration = Duration.ofSeconds(10);
  private final Duration subWindowDuration = Duration.ofSeconds(2);

  @BeforeEach
  @AfterEach
  void setup() {
    long now = Instant.now().toEpochMilli();
    long currentSubWindowNumber = now / subWindowDuration.toMillis();
    long windowSubWindowCount = windowDuration.toMillis() / subWindowDuration.toMillis();

    for (long i = 0; i <= windowSubWindowCount; i++) {
      long subWindowNumber = currentSubWindowNumber - i;
      String subWindowKey = String.format("sliding_window_counter:%s:%s:%s", id, key, subWindowNumber);
      redisCommands.del(subWindowKey.getBytes());
    }
  }

  @Test
  void shouldAllowRequestsWithinLimit() {
    for (int i = 0; i < limit; i++) {
      boolean allowed = slidingWindowCounterRateLimiter.tryConsume(id, key, limit, windowDuration, subWindowDuration);
      assertTrue(allowed, "Requests below the limit must be accepted during the window period.");
    }
  }

  @Test
  void shouldRejectRequestsOverLimitInSameWindow() {
    for (int i = 0; i < limit; i++) {
      slidingWindowCounterRateLimiter.tryConsume(id, key, limit, windowDuration, subWindowDuration);
    }
    boolean allowed = slidingWindowCounterRateLimiter.tryConsume(id, key, limit, windowDuration, subWindowDuration);
    assertFalse(allowed, "Requests should be rejected when the limit is exceeded.");
  }

  @Test
  void shouldResetWindowAndAcceptRequestsAgain() throws InterruptedException {
    for (int i = 0; i < limit; i++) {
      slidingWindowCounterRateLimiter.tryConsume(id, key, limit, windowDuration, subWindowDuration);
    }
    assertFalse(slidingWindowCounterRateLimiter.tryConsume(id, key, limit, windowDuration, subWindowDuration));
    // Wait until window expire
    Thread.sleep(windowDuration.toMillis() + 200);
    boolean allowed = slidingWindowCounterRateLimiter.tryConsume(id, key, limit, windowDuration, subWindowDuration);
    assertTrue(allowed, "Request should be accepted when window expire.");
  }

  @Test
  void shouldHandleMultipleKeysSeparately() {
    String anotherKey = "127.0.0.2";
    for (int i = 0; i < limit; i++) {
      slidingWindowCounterRateLimiter.tryConsume(id, key, limit, windowDuration, subWindowDuration);
    }
    for (int i = 0; i < limit; i++) {
      boolean allowed = slidingWindowCounterRateLimiter.tryConsume(id, anotherKey, limit, windowDuration, subWindowDuration);
      assertTrue(allowed, "There should be a separate window for each key.");
    }
  }

  @Test
  void shouldHandleMultipleIdsSeparately() {
    String anotherId = "AnotherSlidingWindowCounterRateLimiterTest";
    for (int i = 0; i < limit; i++) {
      slidingWindowCounterRateLimiter.tryConsume(id, key, limit, windowDuration, subWindowDuration);
    }
    for (int i = 0; i < limit; i++) {
      boolean allowed = slidingWindowCounterRateLimiter.tryConsume(anotherId, key, limit, windowDuration, subWindowDuration);
      assertTrue(allowed, "There should be a separate window for each id.");
    }
  }

  @Test
  void shouldRejectRequestsForZeroOrNegativeLimit() {
    boolean allowed = slidingWindowCounterRateLimiter.tryConsume(id, key, 0, windowDuration, subWindowDuration);
    assertFalse(allowed, "Request should be rejected when limit given as zero.");

    allowed = slidingWindowCounterRateLimiter.tryConsume(id, key, -5, windowDuration, subWindowDuration);
    assertFalse(allowed, "Request should be rejected when limit given as negative.");
  }

  @Test
  void shouldThrottleBurstRequestsWithWeightedCounter() throws InterruptedException {
    Thread.sleep(windowDuration.toMillis() - subWindowDuration.toMillis());
    long burst = limit;
    int accepted = 0;
    for (int i = 0; i < burst; i++) {
      if (slidingWindowCounterRateLimiter.tryConsume(id, key, limit, windowDuration, subWindowDuration)) {
        accepted++;
      }
    }
    assertTrue(accepted <= limit);
  }

  @Test
  void shouldBeThreadSafeUnderConcurrency() throws InterruptedException {
    int threads = 20;
    ExecutorService service = Executors.newFixedThreadPool(threads);
    CountDownLatch latch = new CountDownLatch(threads);
    AtomicInteger acceptedCount = new AtomicInteger(0);

    for (int i = 0; i < threads; i++) {
      service.submit(() -> {
        if (slidingWindowCounterRateLimiter.tryConsume(id, key, limit, windowDuration, subWindowDuration)) {
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