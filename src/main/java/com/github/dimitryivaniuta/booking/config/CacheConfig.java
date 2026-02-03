package com.github.dimitryivaniuta.booking.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

/**
 * Redis-backed cache configuration.
 *
 * <p>We cache read-side availability search results because these queries are often hot and repeated
 * (same city/capacity/date window). On any booking write, we invalidate the availability cache
 * after the transaction commits.
 */
@Configuration
@EnableCaching
public class CacheConfig {

  /** Cache name for availability search results. */
  public static final String AVAILABILITY_SEARCH_CACHE = "availabilitySearch";

  /**
   * Cache manager configured for Redis.
   *
   * <p>Important: we keep TTL short for availability to reduce staleness and to minimize large key
   * accumulation under high-cardinality query combinations.
   */
  @Bean
  public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    RedisSerializationContext.SerializationPair<Object> valueSerializer =
        RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer());

    RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
        .serializeValuesWith(valueSerializer)
        .disableCachingNullValues()
        .entryTtl(Duration.ofMinutes(10));

    Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
    perCache.put(AVAILABILITY_SEARCH_CACHE, defaults.entryTtl(Duration.ofSeconds(30)));

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(defaults)
        .withInitialCacheConfigurations(perCache)
        .transactionAware()
        .build();
  }
}
