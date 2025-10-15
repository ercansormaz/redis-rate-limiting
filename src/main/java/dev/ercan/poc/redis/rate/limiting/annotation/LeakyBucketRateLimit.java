package dev.ercan.poc.redis.rate.limiting.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LeakyBucketRateLimit {

  String id();
  String key();
  long capacity();
  long leakRate();
  String leakPeriod();

}
