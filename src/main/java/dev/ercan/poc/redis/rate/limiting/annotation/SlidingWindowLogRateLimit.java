package dev.ercan.poc.redis.rate.limiting.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SlidingWindowLogRateLimit {

  String id();
  String key();
  long limit();
  String windowDuration();

}
