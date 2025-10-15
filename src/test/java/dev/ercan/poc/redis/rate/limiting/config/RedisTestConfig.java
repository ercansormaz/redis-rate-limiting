package dev.ercan.poc.redis.rate.limiting.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.ServerSocket;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.embedded.RedisServer;

@Configuration
@Profile("test")
public class RedisTestConfig {

  private RedisServer redisServer;
  private int port;

  @PostConstruct
  public void startRedis() throws IOException {
    port = findAvailablePort();
    redisServer = new RedisServer(port);
    redisServer.start();
    System.setProperty("spring.redis.port", String.valueOf(port));
  }

  @PreDestroy
  public void stopRedis() throws IOException {
    if (redisServer != null) {
      redisServer.stop();
    }
  }

  private int findAvailablePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

}