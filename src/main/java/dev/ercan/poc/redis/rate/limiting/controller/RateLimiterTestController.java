package dev.ercan.poc.redis.rate.limiting.controller;

import dev.ercan.poc.redis.rate.limiting.annotation.FixedWindowRateLimit;
import dev.ercan.poc.redis.rate.limiting.annotation.LeakyBucketRateLimit;
import dev.ercan.poc.redis.rate.limiting.annotation.SlidingWindowCounterRateLimit;
import dev.ercan.poc.redis.rate.limiting.annotation.SlidingWindowLogRateLimit;
import dev.ercan.poc.redis.rate.limiting.annotation.TokenBucketRateLimit;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rate-limiter")
@RequiredArgsConstructor
public class RateLimiterTestController {

  @GetMapping("/token-bucket")
  @TokenBucketRateLimit(id = "testController", key = "#request.remoteAddr", capacity = 3, refillRate = 1, refillPeriod = "5s")
  public ResponseEntity<?> testTokenBucketLimiter(HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.ACCEPTED).build();
  }

  @GetMapping("/leaky-bucket")
  @LeakyBucketRateLimit(id = "testController", key = "#request.remoteAddr", capacity = 3, leakRate = 1, leakPeriod = "5s")
  public ResponseEntity<?> testLeakyBucketLimiter(HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.ACCEPTED).build();
  }

  @GetMapping("/fixed-window")
  @FixedWindowRateLimit(id = "testController", key = "#request.remoteAddr", limit = 3, windowDuration = "10s")
  public ResponseEntity<?> testFixedWindowLimiter(HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.ACCEPTED).build();
  }

  @GetMapping("/sliding-window-log")
  @SlidingWindowLogRateLimit(id = "testController", key = "#request.remoteAddr", limit = 3, windowDuration = "10s")
  public ResponseEntity<?> testSlidingWindowLogLimiter(HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.ACCEPTED).build();
  }

  @GetMapping("/sliding-window-counter")
  @SlidingWindowCounterRateLimit(id = "testController", key = "#request.remoteAddr", limit = 3, windowDuration = "10s", subWindowDuration = "1s")
  public ResponseEntity<?> testSlidingWindowCounterLimiter(HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.ACCEPTED).build();
  }

}
