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
class LeakyBucketRateLimiterTest {

  @Autowired
  private LeakyBucketRateLimiter leakyBucketRateLimiter;

  @Autowired
  private RedisCommands redisCommands;

  private final String id = "LeakyBucketRateLimiterTest";
  private final String key = "127.0.0.1";
  private final long capacity = 5;
  private final long leakRate = 1;
  private final Duration leakPeriod = Duration.ofSeconds(2);

  @BeforeEach
  @AfterEach
  void setup() {
    redisCommands.del(("leaky_bucket:" + id + ":" + key).getBytes());
  }

  @Test
  void shouldAllowRequestsWhenBucketNotFull() {
    for (int i = 0; i < capacity; i++) {
      boolean allowed = leakyBucketRateLimiter.tryConsume(id, key, capacity, leakRate, leakPeriod);
      assertTrue(allowed, "Requests should be accepted when bucket is not full");
    }
  }

  @Test
  void shouldRejectRequestsWhenBucketFull() {
    for (int i = 0; i < capacity; i++) {
      leakyBucketRateLimiter.tryConsume(id, key, capacity, leakRate, leakPeriod);
    }
    boolean allowed = leakyBucketRateLimiter.tryConsume(id, key, capacity, leakRate, leakPeriod);
    assertFalse(allowed, "Requests should be rejected when bucket is full");
  }

  @Test
  void shouldLeakAndAcceptNewRequestsAfterPeriod() throws InterruptedException {
    for (int i = 0; i < capacity; i++) {
      leakyBucketRateLimiter.tryConsume(id, key, capacity, leakRate, leakPeriod);
    }
    assertFalse(leakyBucketRateLimiter.tryConsume(id, key, capacity, leakRate, leakPeriod));
    // Wait for the leakage time
    Thread.sleep(leakPeriod.toMillis() + 200);
    boolean allowed = leakyBucketRateLimiter.tryConsume(id, key, capacity, leakRate, leakPeriod);
    assertTrue(allowed, "Requests should be accepted after leak period.");
  }

  @Test
  void shouldHandleMultipleKeysSeparately() {
    String anotherKey = "127.0.0.2";
    for (int i = 0; i < capacity; i++) {
      leakyBucketRateLimiter.tryConsume(id, key, capacity, leakRate, leakPeriod);
    }
    for (int i = 0; i < capacity; i++) {
      boolean allowed = leakyBucketRateLimiter.tryConsume(id, anotherKey, capacity, leakRate, leakPeriod);
      assertTrue(allowed, "There should be a separate bucket for each key.");
    }
  }

  @Test
  void shouldHandleMultipleIdsSeparately() {
    String anotherId = "AnotherLeakyBucketRateLimiterTest";
    for (int i = 0; i < capacity; i++) {
      leakyBucketRateLimiter.tryConsume(id, key, capacity, leakRate, leakPeriod);
    }
    for (int i = 0; i < capacity; i++) {
      boolean allowed = leakyBucketRateLimiter.tryConsume(anotherId, key, capacity, leakRate, leakPeriod);
      assertTrue(allowed, "There should be a separate bucket for each id.");
    }
  }

  @Test
  void shouldBeThreadSafeUnderConcurrency() throws InterruptedException {
    int threads = 20;
    ExecutorService service = Executors.newFixedThreadPool(threads);
    CountDownLatch latch = new CountDownLatch(threads);
    AtomicInteger acceptedCount = new AtomicInteger(0);

    for (int i = 0; i < threads; i++) {
      service.submit(() -> {
        if (leakyBucketRateLimiter.tryConsume(id, key, capacity, leakRate, leakPeriod)) {
          acceptedCount.incrementAndGet();
        }
        latch.countDown();
      });
    }
    latch.await();
    service.shutdown();

    assertEquals(capacity, acceptedCount.get(),
        "The capacity should not be exceeded for parallel requests.");
  }
}