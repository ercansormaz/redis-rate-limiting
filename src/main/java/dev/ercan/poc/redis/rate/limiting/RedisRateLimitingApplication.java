package dev.ercan.poc.redis.rate.limiting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class RedisRateLimitingApplication {

	public static void main(String[] args) {
		SpringApplication.run(RedisRateLimitingApplication.class, args);
	}

}
