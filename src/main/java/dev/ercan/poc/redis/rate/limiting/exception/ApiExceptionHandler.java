package dev.ercan.poc.redis.rate.limiting.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(value = RateLimitExceedException.class)
  public ResponseEntity<?> rateLimitExceedException(RateLimitExceedException exception) {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
  }

}