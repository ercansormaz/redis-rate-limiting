package dev.ercan.poc.redis.rate.limiting.service;

import io.lettuce.core.RedisNoScriptException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
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
public class SlidingWindowLogRateLimiter {

  private static final String WINDOW_KEY = "sliding_window_log:%s:%s";

  private final RedisCommands redisCommands;
  private final RedisScript<Long> slidingWindowLogRateLimiterScript;

  public boolean tryConsume(String id, String key, long limit, Duration windowDuration) {
    if (limit < 1) {
      return false;
    }

    byte[][] keysAndArgs = {
        String.format(WINDOW_KEY, id, key).getBytes(),
        Long.toString(Instant.now().toEpochMilli()).getBytes(),
        Long.toString(windowDuration.toMillis()).getBytes(),
        Long.toString(limit).getBytes(),
        UUID.randomUUID().toString().getBytes()
    };

    Long result;

    try {
      String scriptSha1 = slidingWindowLogRateLimiterScript.getSha1();
      result = redisCommands.evalSha(scriptSha1, ReturnType.INTEGER, 1, keysAndArgs);
    } catch (RedisSystemException ex) {
      if (ex.getCause() instanceof RedisNoScriptException) {
        byte[] scriptSource = slidingWindowLogRateLimiterScript.getScriptAsString().getBytes();
        result = redisCommands.eval(scriptSource, ReturnType.INTEGER, 1, keysAndArgs);
      } else {
        throw ex;
      }
    }

    return result == 1;
  }

  /*
  public boolean tryConsume(String id, String key, long limit, Duration windowDuration) {
    String windowKey = String.format(WINDOW_KEY, id, key);

    long now = Instant.now().getEpochSecond();
    long windowSeconds = windowDuration.getSeconds();

    // remove timestamps outside window
    redisTemplate.opsForZSet().removeRangeByScore(windowKey, 0, now - windowSeconds);

    // get count within window
    Long count = redisTemplate.opsForZSet().count(windowKey, (now - windowSeconds) + 1, now);

    // Add current timestamp to sorted set if request allowed
    if (count != null && count < limit) {
      redisTemplate.opsForZSet().add(windowKey, UUID.randomUUID().toString(), now);
      redisTemplate.expire(windowKey, windowDuration);
      return true;
    }

    return false;
  }
  */

}
