package dev.ercan.poc.redis.rate.limiting.service;

import io.lettuce.core.RedisNoScriptException;
import java.time.Duration;
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
public class FixedWindowRateLimiter {

  private static final String WINDOW_KEY = "fixed_window:%s:%s";

  private final RedisCommands redisCommands;
  private final RedisScript<Long> fixedWindowRateLimiterScript;

  public boolean tryConsume(String id, String key, long limit, Duration windowDuration) {
    byte[][] keysAndArgs = {
        String.format(WINDOW_KEY, id, key).getBytes(),
        Long.toString(windowDuration.toMillis()).getBytes()
    };

    Long count;

    try {
      String scriptSha1 = fixedWindowRateLimiterScript.getSha1();
      count = redisCommands.evalSha(scriptSha1, ReturnType.INTEGER, 1, keysAndArgs);
    } catch (RedisSystemException ex) {
      if (ex.getCause() instanceof RedisNoScriptException) {
        byte[] scriptSource = fixedWindowRateLimiterScript.getScriptAsString().getBytes();
        count = redisCommands.eval(scriptSource, ReturnType.INTEGER, 1, keysAndArgs);
      } else {
        throw ex;
      }
    }

    return count <= limit;
  }

}
