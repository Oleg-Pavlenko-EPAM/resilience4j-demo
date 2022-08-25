package com.vertexinc.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/rest")
public class TestController {

  private final static Logger LOGGER = LoggerFactory.getLogger(TestController.class);

  @Autowired
  private TestService testService;

  private void execute(Runnable service) {
    try {
      service.run();
      LOGGER.info("Controller got successful response");
    } catch (NumberFormatException ex) {
      LOGGER.info("Controller got exception: {} - {}", ex.getClass().getSimpleName(), ex.getMessage());
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    } catch (RuntimeException ex) {
      LOGGER.info("Controller got exception: {} - {}", ex.getClass().getSimpleName(), ex.getMessage());
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
    }
  }

  @GetMapping("/circuit-breaker")
  public void testCircuitBreaker() {
    execute(() -> testService.testCircuitBreaker());
  }

  @GetMapping("/circuit-breaker-dynamic/{name}")
  public void testCircuitBreakerDynamic(@PathVariable String name) {
    execute(() -> testService.testCircuitBreakerDynamic(name));
  }

  @GetMapping("/circuit-breaker-with-fallback/{input}")
  public void testCircuitBreakerFallback(@PathVariable String input) {
    execute(() -> testService.testCircuitBreakerWithFallback(input));
  }

  @PostMapping(value = "/circuit-breaker/failure-rate", consumes = MediaType.TEXT_PLAIN_VALUE)
  public void setCircuitBreakerFailureRate(@RequestBody String failureRate) {
    testService.setCircuitBreakerFailureRate(Integer.parseInt(failureRate));
  }

  @GetMapping("/circuit-breaker-time-based-window")
  public void testCircuitBreakerTimeBasedWindow() {
    execute(() -> testService.testCircuitBreakerTimeBased());
  }

  @GetMapping("/circuit-breaker-ignorable-exception/{input}")
  public void testCircuitBreakerIgnorableException(@PathVariable String input) {
    execute(() -> testService.testCircuitBreakerIgnorableException(input));
  }

  @GetMapping("/circuit-breaker-webflux")
  public Mono<Void> testCircuitBreakerWebflux() {
    return testService.testCircuitBreakerWebflux()
        .doOnSuccess(v -> {
          LOGGER.info("Controller got successful response");
        })
        .doOnError(ex -> {
          LOGGER.info("Controller got exception: {} - {}", ex.getClass().getSimpleName(), ex.getMessage());
          throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        });
  }

  @GetMapping("/bulkhead")
  public void testBulkhead() {
    execute(() -> testService.testBulkhead());
  }

  @PostMapping(value = "/bulkhead/wait-duration", consumes = MediaType.TEXT_PLAIN_VALUE)
  public void setBulkheadWaitDuration(@RequestBody String waitDuration) {
    testService.setBulkheadWaitDuration(Integer.parseInt(waitDuration));
  }

  @GetMapping("/rate-limiter")
  public void testRateLimiter() {
    execute(() -> testService.testRateLimiter());
  }

  @GetMapping("/time-limiter")
  public DeferredResult<Void> testTimeLimiter() {
    DeferredResult<Void> result = new DeferredResult<>();

    testService.testTimeLimiter()
        .thenAccept(v -> {
          LOGGER.info("success");
          result.setResult(v);
        })
        .exceptionally(ex -> {
          LOGGER.info("{}: {}", ex.getClass(), ex.getMessage());
          result.setErrorResult(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE));
          return null;
        });

    return result;
  }

  @PostMapping(value = "/time-limiter/wait-duration", consumes = MediaType.TEXT_PLAIN_VALUE)
  public void setTimeLimiterWaitDuration(@RequestBody String waitDuration) {
    testService.setTimeLimiterWaitDuration(Integer.parseInt(waitDuration));
  }

  @GetMapping("/retry")
  public void testRetry() {
    execute(() -> testService.testRetry());
  }

  @PostMapping(value = "/retry/failure-rate", consumes = MediaType.TEXT_PLAIN_VALUE)
  public void setRetryFailureRate(@RequestBody String failureRate) {
    testService.setRetryFailureRate(Integer.parseInt(failureRate));
  }
}
