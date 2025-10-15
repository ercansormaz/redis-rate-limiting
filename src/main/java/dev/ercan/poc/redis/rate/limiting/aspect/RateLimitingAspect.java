package dev.ercan.poc.redis.rate.limiting.aspect;

import dev.ercan.poc.redis.rate.limiting.annotation.FixedWindowRateLimit;
import dev.ercan.poc.redis.rate.limiting.annotation.LeakyBucketRateLimit;
import dev.ercan.poc.redis.rate.limiting.annotation.SlidingWindowCounterRateLimit;
import dev.ercan.poc.redis.rate.limiting.annotation.SlidingWindowLogRateLimit;
import dev.ercan.poc.redis.rate.limiting.annotation.TokenBucketRateLimit;
import dev.ercan.poc.redis.rate.limiting.exception.RateLimitExceedException;
import dev.ercan.poc.redis.rate.limiting.service.FixedWindowRateLimiter;
import dev.ercan.poc.redis.rate.limiting.service.LeakyBucketRateLimiter;
import dev.ercan.poc.redis.rate.limiting.service.SlidingWindowCounterRateLimiter;
import dev.ercan.poc.redis.rate.limiting.service.SlidingWindowLogRateLimiter;
import dev.ercan.poc.redis.rate.limiting.service.TokenBucketRateLimiter;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitingAspect {

  private final FixedWindowRateLimiter fixedWindowRateLimiter;
  private final TokenBucketRateLimiter tokenBucketRateLimiter;
  private final LeakyBucketRateLimiter leakyBucketRateLimiter;
  private final SlidingWindowCounterRateLimiter slidingWindowCounterRateLimiter;
  private final SlidingWindowLogRateLimiter slidingWindowLogRateLimiter;

  @Around("@annotation(fixedWindowRateLimit)")
  public Object fixedWindowRateLimit(ProceedingJoinPoint pjp,
      FixedWindowRateLimit fixedWindowRateLimit) throws Throwable {
    String key = getSpELKeyValue(pjp, fixedWindowRateLimit.key());
    Duration windowDuration = DurationStyle.detectAndParse(fixedWindowRateLimit.windowDuration());

    if (fixedWindowRateLimiter.tryConsume(fixedWindowRateLimit.id(), key,
        fixedWindowRateLimit.limit(), windowDuration)) {
      return pjp.proceed();
    }

    throw new RateLimitExceedException();
  }

  @Around("@annotation(tokenBucketRateLimit)")
  public Object tokenBucketLimit(ProceedingJoinPoint pjp, TokenBucketRateLimit tokenBucketRateLimit)
      throws Throwable {
    String key = getSpELKeyValue(pjp, tokenBucketRateLimit.key());
    Duration refillPeriod = DurationStyle.detectAndParse(tokenBucketRateLimit.refillPeriod());

    if (tokenBucketRateLimiter.tryConsume(tokenBucketRateLimit.id(), key,
        tokenBucketRateLimit.capacity(), tokenBucketRateLimit.refillRate(), refillPeriod)) {
      return pjp.proceed();
    }

    throw new RateLimitExceedException();
  }

  @Around("@annotation(leakyBucketRateLimit)")
  public Object leakyBucketRateLimit(ProceedingJoinPoint pjp, LeakyBucketRateLimit leakyBucketRateLimit)
      throws Throwable {
    String key = getSpELKeyValue(pjp, leakyBucketRateLimit.key());
    Duration leakPeriod = DurationStyle.detectAndParse(leakyBucketRateLimit.leakPeriod());

    if (leakyBucketRateLimiter.tryConsume(leakyBucketRateLimit.id(), key,
        leakyBucketRateLimit.capacity(), leakyBucketRateLimit.leakRate(), leakPeriod)) {
      return pjp.proceed();
    }

    throw new RateLimitExceedException();
  }

  @Around("@annotation(slidingWindowCounterRateLimit)")
  public Object slidingWindowCounterRateLimit(ProceedingJoinPoint pjp, SlidingWindowCounterRateLimit slidingWindowCounterRateLimit)
      throws Throwable {
    String key = getSpELKeyValue(pjp, slidingWindowCounterRateLimit.key());
    Duration windowDuration = DurationStyle.detectAndParse(slidingWindowCounterRateLimit.windowDuration());
    Duration subWindowDuration = DurationStyle.detectAndParse(slidingWindowCounterRateLimit.subWindowDuration());

    if (slidingWindowCounterRateLimiter.tryConsume(slidingWindowCounterRateLimit.id(), key,
        slidingWindowCounterRateLimit.limit(), windowDuration, subWindowDuration)) {
      return pjp.proceed();
    }

    throw new RateLimitExceedException();
  }

  @Around("@annotation(slidingWindowLogRateLimit)")
  public Object slidingWindowLogRateLimit(ProceedingJoinPoint pjp, SlidingWindowLogRateLimit slidingWindowLogRateLimit)
      throws Throwable {
    String key = getSpELKeyValue(pjp, slidingWindowLogRateLimit.key());
    Duration windowDuration = DurationStyle.detectAndParse(slidingWindowLogRateLimit.windowDuration());

    if (slidingWindowLogRateLimiter.tryConsume(slidingWindowLogRateLimit.id(), key,
        slidingWindowLogRateLimit.limit(), windowDuration)) {
      return pjp.proceed();
    }

    throw new RateLimitExceedException();
  }

  private String getSpELKeyValue(ProceedingJoinPoint pjp, String spelKey) {
    MethodSignature signature = (MethodSignature) pjp.getSignature();

    EvaluationContext context = new StandardEvaluationContext();
    Object[] args = pjp.getArgs();
    String[] paramNames = signature.getParameterNames();
    for (int i = 0; i < args.length; i++) {
      context.setVariable(paramNames[i], args[i]);
    }

    ExpressionParser parser = new SpelExpressionParser();
    return parser.parseExpression(spelKey).getValue(context, String.class);
  }

}
