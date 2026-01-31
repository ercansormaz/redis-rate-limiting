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
public class LeakyBucketRateLimiter {

  private static final String BUCKET_KEY = "leaky_bucket:%s:%s";

  private final RedisCommands redisCommands;
  private final RedisScript<Long> leakyBucketRateLimiterScript;

  public boolean tryConsume(String id, String key, long capacity, long leakRate, Duration leakPeriod) {
    long expireIn = leakPeriod.multipliedBy(capacity / leakRate).toMillis();

    byte[][] keysAndArgs = {
        String.format(BUCKET_KEY, id, key).getBytes(),
        Long.toString(Instant.now().toEpochMilli()).getBytes(),
        Long.toString(capacity).getBytes(),
        Long.toString(leakRate).getBytes(),
        Long.toString(leakPeriod.toMillis()).getBytes(),
        Long.toString(expireIn).getBytes()
    };

    Long result;

    try {
      String scriptSha1 = leakyBucketRateLimiterScript.getSha1();
      result = redisCommands.evalSha(scriptSha1, ReturnType.INTEGER, 1, keysAndArgs);
    } catch (RedisSystemException ex) {
      if (ex.getCause() instanceof RedisNoScriptException) {
        byte[] scriptSource = leakyBucketRateLimiterScript.getScriptAsString().getBytes();
        result = redisCommands.eval(scriptSource, ReturnType.INTEGER, 1, keysAndArgs);
      } else {
        throw ex;
      }
    }

    return result == 1;
  }

}
