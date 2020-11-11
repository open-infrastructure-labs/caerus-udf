package org.openinfralabs.caerus.UdfRegistry.config;

import org.openinfralabs.caerus.UdfRegistry.controller.UdfController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class RedisConfig {

    Logger logger = LoggerFactory.getLogger(UdfController.class);

    private final String HOSTNAME;

    private final int PORT;

    private final int DATABASE;

    private final String PASSWORD;

    private final long TIMEOUT;

    public RedisConfig(
            @Value("${redis.hostname}") String hostname,
            @Value("${redis.port}") int port,
            @Value("${redis.database}") int database,
            @Value("${redis.password}") String password,
            @Value("${redis.timeout}") long timeout) {

        this.HOSTNAME = hostname;
        this.PORT = port;
        this.DATABASE = database;
        this.PASSWORD = password;
        this.TIMEOUT = timeout;
        logger.info("hostname = "+hostname + " port =" + String.valueOf(port));
    }

    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {

        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(HOSTNAME);
        redisStandaloneConfiguration.setPort(PORT);
        redisStandaloneConfiguration.setPassword(PASSWORD);

        JedisConnectionFactory jedisConFactory
                = new JedisConnectionFactory(redisStandaloneConfiguration);
        return jedisConFactory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {

        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(jedisConnectionFactory());
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new JdkSerializationRedisSerializer());
        redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
        redisTemplate.setEnableTransactionSupport(true);
        return redisTemplate;
    }

}
