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
public class SlidingWindowCounterRateLimiter {

  private static final String WINDOW_KEY = "sliding_window_counter:%s:%s:%s";

  private final RedisCommands redisCommands;
  private final RedisScript<Long> slidingWindowCounterRateLimiterScript;

  public boolean tryConsume(String id, String key, long limit, Duration windowDuration, Duration subWindowDuration) {
    long now = Instant.now().toEpochMilli();
    long currentSubWindowNumber = now / subWindowDuration.toMillis();
    long previousSubWindowNumber = currentSubWindowNumber - 1;

    byte[][] keysAndArgs = {
        String.format(WINDOW_KEY, id, key, currentSubWindowNumber).getBytes(),
        String.format(WINDOW_KEY, id, key, previousSubWindowNumber).getBytes(),
        Long.toString(now).getBytes(),
        Long.toString(windowDuration.toMillis()).getBytes(),
        Long.toString(subWindowDuration.toMillis()).getBytes(),
        Long.toString(limit).getBytes()
    };

    Long result;

    try {
      String scriptSha1 = slidingWindowCounterRateLimiterScript.getSha1();
      result = redisCommands.evalSha(scriptSha1, ReturnType.INTEGER, 2, keysAndArgs);
    } catch (RedisSystemException ex) {
      if (ex.getCause() instanceof RedisNoScriptException) {
        byte[] scriptSource = slidingWindowCounterRateLimiterScript.getScriptAsString().getBytes();
        result = redisCommands.eval(scriptSource, ReturnType.INTEGER, 2, keysAndArgs);
      } else {
        throw ex;
      }
    }

    return result == 1;
  }

}
