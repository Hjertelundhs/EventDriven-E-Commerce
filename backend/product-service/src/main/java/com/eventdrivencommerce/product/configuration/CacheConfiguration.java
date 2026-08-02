package com.eventdrivencommerce.product.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@Configuration
public class CacheConfiguration {

    @Bean
    CacheManager productCacheManager(
            RedisConnectionFactory connectionFactory,
            ProductCacheProperties properties,
            ObjectMapper objectMapper) {
        GenericJackson2JsonRedisSerializer valueSerializer = GenericJackson2JsonRedisSerializer.builder()
                .objectMapper(objectMapper.copy())
                .defaultTyping(true)
                .build();
        RedisCacheConfiguration products = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(properties.timeToLive())
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer));
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(products)
                .withInitialCacheConfigurations(Map.of("products", products))
                .transactionAware()
                .build();
    }

    @ConfigurationProperties(prefix = "product.cache")
    public record ProductCacheProperties(Duration timeToLive) {
        public ProductCacheProperties {
            timeToLive = timeToLive == null ? Duration.ofMinutes(10) : timeToLive;
        }
    }
}
