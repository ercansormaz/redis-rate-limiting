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
class TokenBucketRateLimiterTest {

  @Autowired
  private TokenBucketRateLimiter tokenBucketRateLimiter;

  @Autowired
  private RedisCommands redisCommands;

  private final String id = "TokenBucketRateLimiterTest";
  private final String key = "127.0.0.1";
  private final long capacity = 5;
  private final long refillRate = 1;
  private final Duration refillPeriod = Duration.ofSeconds(2);

  @BeforeEach
  @AfterEach
  void setup() {
    redisCommands.del(("token_bucket:" + id + ":" + key).getBytes());
  }

  @Test
  void shouldAllowRequestsWhenTokensAvailable() {
    for (int i = 0; i < capacity; i++) {
      boolean allowed = tokenBucketRateLimiter.tryConsume(id, key, capacity, refillRate,
          refillPeriod);
      assertTrue(allowed, "Requests below the limit must be accepted while avaliable token exists.");
    }
  }

  @Test
  void shouldRejectRequestsWhenTokensExhausted() {
    for (int i = 0; i < capacity; i++) {
      tokenBucketRateLimiter.tryConsume(id, key, capacity, refillRate, refillPeriod);
    }
    boolean allowed = tokenBucketRateLimiter.tryConsume(id, key, capacity, refillRate,
        refillPeriod);
    assertFalse(allowed, "Requests should be rejected while avaliable token not exists.");
  }

  @Test
  void shouldRefillTokensAfterPeriod() throws InterruptedException {
    for (int i = 0; i < capacity; i++) {
      tokenBucketRateLimiter.tryConsume(id, key, capacity, refillRate, refillPeriod);
    }
    assertFalse(tokenBucketRateLimiter.tryConsume(id, key, capacity, refillRate, refillPeriod));

    // Wait refill period
    Thread.sleep(refillPeriod.toMillis() + 200);

    // Should be allowed after refillPeriod
    boolean allowed = tokenBucketRateLimiter.tryConsume(id, key, capacity, refillRate,
        refillPeriod);
    assertTrue(allowed, "Request should be accepted after refill period.");
  }

  @Test
  void shouldNotExceedCapacityWithBurstRequests() {
    int burst = 10;
    int accepted = 0;
    for (int i = 0; i < burst; i++) {
      if (tokenBucketRateLimiter.tryConsume(id, key, capacity, refillRate, refillPeriod)) {
        accepted++;
      }
    }
    assertEquals(capacity, accepted, "The capacity should not be exceeded for burst requests.");
  }

  @Test
  void shouldNotAllowNegativeOrZeroCapacity() {
    boolean allowed = tokenBucketRateLimiter.tryConsume(id, key, 0, refillRate, refillPeriod);
    assertFalse(allowed, "Request should be rejected when capacity given as zero.");

    allowed = tokenBucketRateLimiter.tryConsume(id, key, -5, refillRate, refillPeriod);
    assertFalse(allowed, "Request should be rejected when capacity given as negative.");
  }

  @Test
  void shouldHandleMultipleKeysSeparately() {
    String anotherKey = "127.0.0.2";
    for (int i = 0; i < capacity; i++) {
      tokenBucketRateLimiter.tryConsume(id, key, capacity, refillRate, refillPeriod);
    }
    for (int i = 0; i < capacity; i++) {
      boolean allowed = tokenBucketRateLimiter.tryConsume(id, anotherKey, capacity, refillRate,
          refillPeriod);
      assertTrue(allowed, "There should be a separate bucket for each key.");
    }
  }

  @Test
  void shouldHandleMultipleIdsSeparately() {
    String anotherId = "AnotherTokenBucketRateLimiterTest";
    for (int i = 0; i < capacity; i++) {
      tokenBucketRateLimiter.tryConsume(id, key, capacity, refillRate, refillPeriod);
    }
    for (int i = 0; i < capacity; i++) {
      boolean allowed = tokenBucketRateLimiter.tryConsume(anotherId, key, capacity, refillRate,
          refillPeriod);
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
        if (tokenBucketRateLimiter.tryConsume(id, key, capacity, refillRate, refillPeriod)) {
          acceptedCount.incrementAndGet();
        }
        latch.countDown();
      });
    }
    latch.await();
    service.shutdown();

    assertEquals(capacity, acceptedCount.get(), "The capacity should not be exceeded for parallel requests.");
  }

}