package dev.ercan.poc.redis.rate.limiting.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisCommands;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
public class RedisConfig {

  @Bean
  public RedisCommands redisCommands(RedisConnectionFactory connectionFactory) {
    return connectionFactory.getConnection().commands();
  }

}
