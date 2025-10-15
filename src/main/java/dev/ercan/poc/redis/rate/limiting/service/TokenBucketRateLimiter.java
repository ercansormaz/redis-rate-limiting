package dev.ercan.poc.redis.rate.limiting.service;

import io.lettuce.core.RedisNoScriptException;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisCommands;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBucketRateLimiter {

  private static final String BUCKET_KEY = "token_bucket:%s:%s";

  private final RedisCommands redisCommands;
  private final RedisScript<Long> tokenBucketRateLimiterScript;

  public boolean tryConsume(String id, String key, long capacity, long refillRate,
      Duration refillPeriod) {

    if (capacity < 1) {
      return false;
    }

    long expireIn = refillPeriod.multipliedBy(capacity / refillRate).toMillis();

    byte[][] keysAndArgs = {
        String.format(BUCKET_KEY, id, key).getBytes(),
        Long.toString(Instant.now().toEpochMilli()).getBytes(),
        Long.toString(capacity).getBytes(),
        Long.toString(refillRate).getBytes(),
        Long.toString(refillPeriod.toMillis()).getBytes(),
        Long.toString(expireIn).getBytes()
    };

    Long result;

    try {
      String scriptSha1 = tokenBucketRateLimiterScript.getSha1();
      result = redisCommands.evalSha(scriptSha1, ReturnType.INTEGER, 1, keysAndArgs);
    } catch (RedisSystemException ex) {
      if (ex.getCause() instanceof RedisNoScriptException) {
        byte[] scriptSource = tokenBucketRateLimiterScript.getScriptAsString().getBytes();
        result = redisCommands.eval(scriptSource, ReturnType.INTEGER, 1, keysAndArgs);
      } else {
        throw ex;
      }
    }

    return result == 1;
  }

  /*
  public boolean tryConsume(String id, String key, long capacity, long refillRate,
      Duration refillPeriod) {
    String bucketKey = String.format(BUCKET_KEY, id, key);

    String value = redisTemplate.opsForValue().get(bucketKey);

    long now = Instant.now().toEpochMilli();

    long tokens, lastRefill;
    if (!StringUtils.hasLength(value)) {
      tokens = capacity;
      lastRefill = now;
    } else {
      String[] parts = value.split(":");
      tokens = Long.parseLong(parts[0]);
      lastRefill = Long.parseLong(parts[1]);

      long intervals = (now - lastRefill) / refillPeriod.toMillis();

      if (intervals > 0) {
        tokens = Math.min(capacity, tokens + (intervals * refillRate));
        lastRefill = lastRefill + (intervals * refillPeriod.toMillis());
      }
    }

    long expireIn = refillPeriod.multipliedBy(capacity / refillRate).toMillis();

    if (tokens > 0) {
      tokens -= 1;
      redisTemplate.opsForValue().set(bucketKey, tokens + ":" + lastRefill, expireIn, TimeUnit.MILLISECONDS);
      return true;
    }

    redisTemplate.opsForValue().set(bucketKey, tokens + ":" + lastRefill, expireIn, TimeUnit.MILLISECONDS);
    return false;
  }
  */

}
