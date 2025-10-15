package dev.ercan.poc.redis.rate.limiting.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;

@Configuration
public class RedisScriptConfig {

  @Bean
  public RedisScript<Long> tokenBucketRateLimiterScript() {
    ScriptSource scriptSource = new ResourceScriptSource(
        new ClassPathResource("scripts/token_bucket.lua"));

    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
    redisScript.setScriptSource(scriptSource);
    redisScript.setResultType(Long.class);

    return redisScript;
  }

  @Bean
  public RedisScript<Long> leakyBucketRateLimiterScript() {
    ScriptSource scriptSource = new ResourceScriptSource(
        new ClassPathResource("scripts/leaky_bucket.lua"));

    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
    redisScript.setScriptSource(scriptSource);
    redisScript.setResultType(Long.class);

    return redisScript;
  }

  @Bean
  public RedisScript<Long> fixedWindowRateLimiterScript() {
    ScriptSource scriptSource = new ResourceScriptSource(
        new ClassPathResource("scripts/fixed_window.lua"));

    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
    redisScript.setScriptSource(scriptSource);
    redisScript.setResultType(Long.class);

    return redisScript;
  }

  @Bean
  public RedisScript<Long> slidingWindowLogRateLimiterScript() {
    ScriptSource scriptSource = new ResourceScriptSource(
        new ClassPathResource("scripts/sliding_window_log.lua"));

    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
    redisScript.setScriptSource(scriptSource);
    redisScript.setResultType(Long.class);

    return redisScript;
  }

  @Bean
  public RedisScript<Long> slidingWindowCounterRateLimiterScript() {
    ScriptSource scriptSource = new ResourceScriptSource(
        new ClassPathResource("scripts/sliding_window_counter.lua"));

    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
    redisScript.setScriptSource(scriptSource);
    redisScript.setResultType(Long.class);

    return redisScript;
  }

}
