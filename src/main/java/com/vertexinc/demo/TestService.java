package com.vertexinc.demo;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * @author Oleg Pavlenko
 * @version 1.0.0
 */
@Service
public class TestService {

  private final static Logger LOGGER = LoggerFactory.getLogger(TestService.class);

  private final SecureRandom random = new SecureRandom();

  private int circuitBreakerFailureRate = 0;
  private int retryFailureRate = 0;
  private int bulkheadWaitDuration = 20000;
  private int timeLimiterWaitDuration = 10000;

  @Autowired
  private CircuitBreakerRegistry circuitBreakerRegistry;

  public void setCircuitBreakerFailureRate(int failureRate) {
    this.circuitBreakerFailureRate = failureRate;
  }

  @CircuitBreaker(name = "backendA")
  public void testCircuitBreaker() {
    if (random.nextInt(100) < circuitBreakerFailureRate) {
      throw new RuntimeException("The backend service has failed");
    }
  }

  public void testCircuitBreakerDynamic(String name) {
    circuitBreakerRegistry.circuitBreaker(name, "configB").executeRunnable(() -> {
      if (random.nextInt(100) < circuitBreakerFailureRate) {
        throw new RuntimeException("The backend service has failed");
      }
    });
  }

  @CircuitBreaker(name = "backendA", fallbackMethod = "fallback")
  public void testCircuitBreakerWithFallback(String input) {
    LOGGER.info("Normal service is executed: {}", input);
    if (random.nextInt(100) < circuitBreakerFailureRate) {
      throw new RuntimeException("The backend service has failed");
    }
  }

  public void fallback(String input, RuntimeException e) {
    LOGGER.info("Fallback is executed: {} ({}: {})", input, e.getClass().getSimpleName(), e.getMessage());
  }

  @CircuitBreaker(name = "backendC")
  public void testCircuitBreakerTimeBased() {
    if (random.nextInt(100) < circuitBreakerFailureRate) {
      throw new RuntimeException("The backend service has failed");
    }
  }

  @CircuitBreaker(name = "backendC1")
  public void testCircuitBreakerIgnorableException(String input) {
    Integer.parseInt(input);
    if (random.nextInt(100) < circuitBreakerFailureRate) {
      throw new RuntimeException("The backend service has failed");
    }
  }

  @CircuitBreaker(name = "backendA")
  public Mono<Void> testCircuitBreakerWebflux() {
    if (random.nextInt(100) < circuitBreakerFailureRate) {
      return Mono.error(new RuntimeException("The backend service has failed"));
    }
    return Mono.empty();
  }

  public void setBulkheadWaitDuration(int bulkheadWaitDuration) {
    this.bulkheadWaitDuration = bulkheadWaitDuration;
  }

  @Bulkhead(name = "backendA")
  public void testBulkhead() {
    try {
      Thread.sleep(bulkheadWaitDuration);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Bulkhead test service has been interrupted while waiting", e);
    }
  }

  @RateLimiter(name = "backendA")
  public void testRateLimiter() {}

  public void setTimeLimiterWaitDuration(int timeLimiterWaitDuration) {
    this.timeLimiterWaitDuration = timeLimiterWaitDuration;
  }

  @TimeLimiter(name = "backendA")
  public CompletionStage<Void> testTimeLimiter() {
    return CompletableFuture.runAsync(() -> {
      try {
        Thread.sleep(timeLimiterWaitDuration);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Time Limiter test service has been interrupted while waiting", e);
      }
    });
  }

  public void setRetryFailureRate(int retryFailureRate) {
    this.retryFailureRate = retryFailureRate;
  }

  @Retry(name = "backendA")
  public void testRetry() {
    LOGGER.info("Retry test service started");
    if (random.nextInt(100) < retryFailureRate) {
      LOGGER.info("Retry test service finished exceptionally");
      throw new RuntimeException("The backend service has failed");
    } else {
      LOGGER.info("Retry test service finished successfully");
    }
  }
}
