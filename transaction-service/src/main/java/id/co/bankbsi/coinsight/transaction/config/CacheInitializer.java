package id.co.bankbsi.coinsight.transaction.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CacheInitializer implements CommandLineRunner {

  private final CacheManager cacheManager;

  @Override
  public void run(String... args) {
    log.info("Clearing all cache on startup");
    cacheManager
        .getCacheNames()
        .forEach(
            cacheName -> {
              log.debug("Clearing cache: {}", cacheName);
              cacheManager.getCache(cacheName).clear();
            });
  }
}
