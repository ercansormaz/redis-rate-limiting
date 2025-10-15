package dev.ercan.poc.redis.rate.limiting.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimiterControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void tokenBucket_shouldReturnAcceptedUnderLimitAndTooManyRequestsOverLimit() throws Exception {
    for (int i = 0; i < 3; i++) {
      mockMvc.perform(get("/rate-limiter/token-bucket"))
          .andExpect(status().isAccepted());
    }
    mockMvc.perform(get("/rate-limiter/token-bucket"))
        .andExpect(status().isTooManyRequests());
  }

  @Test
  void leakyBucket_shouldReturnAcceptedUnderLimitAndTooManyRequestsOverLimit() throws Exception {
    for (int i = 0; i < 3; i++) {
      mockMvc.perform(get("/rate-limiter/leaky-bucket"))
          .andExpect(status().isAccepted());
    }
    mockMvc.perform(get("/rate-limiter/leaky-bucket"))
        .andExpect(status().isTooManyRequests());
  }

  @Test
  void fixedWindow_shouldReturnAcceptedUnderLimitAndTooManyRequestsOverLimit() throws Exception {
    for (int i = 0; i < 3; i++) {
      mockMvc.perform(get("/rate-limiter/fixed-window"))
          .andExpect(status().isAccepted());
    }
    mockMvc.perform(get("/rate-limiter/fixed-window"))
        .andExpect(status().isTooManyRequests());
  }

  @Test
  void slidingWindowLog_shouldReturnAcceptedUnderLimitAndTooManyRequestsOverLimit() throws Exception {
    for (int i = 0; i < 3; i++) {
      mockMvc.perform(get("/rate-limiter/sliding-window-log"))
          .andExpect(status().isAccepted());
    }
    mockMvc.perform(get("/rate-limiter/sliding-window-log"))
        .andExpect(status().isTooManyRequests());
  }

  @Test
  void slidingWindowCounter_shouldReturnAcceptedUnderLimitAndTooManyRequestsOverLimit() throws Exception {
    for (int i = 0; i < 3; i++) {
      mockMvc.perform(get("/rate-limiter/sliding-window-counter"))
          .andExpect(status().isAccepted());
    }
    mockMvc.perform(get("/rate-limiter/sliding-window-counter"))
        .andExpect(status().isTooManyRequests());
  }

}