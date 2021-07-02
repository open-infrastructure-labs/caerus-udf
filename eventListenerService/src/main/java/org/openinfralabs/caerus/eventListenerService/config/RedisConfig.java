package org.openinfralabs.caerus.eventListenerService.config;

import org.openinfralabs.caerus.eventListenerService.receiver.KeySpaceNotificationMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;


import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executors;

/**
 *
 */
@Configuration
@EnableCaching
public class RedisConfig {
    Logger logger = LoggerFactory.getLogger(RedisConfig.class);
    private final String HOSTNAME;

    private final int PORT;

    private final int DATABASE;

    private final String PASSWORD;

    private final long TIMEOUT;

    /**
     *
     * @param hostname
     * @param port
     * @param database
     * @param password
     * @param timeout
     */
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
        logger.info("hostname=" + hostname);
        logger.info("password=" + password);

    }

    KeySpaceNotificationMessageListener keySpaceNotificationMessageListener;

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
        redisTemplate.setDefaultSerializer(new StringRedisSerializer());
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());
        //redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        //redisTemplate.setHashKeySerializer(new JdkSerializationRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        //redisTemplate.setHashValueSerializer(new StringRedisSerializer());
        redisTemplate.setEnableTransactionSupport(true);
        return redisTemplate;
    }

    @Bean
    MessageListenerAdapter messageListener() {
        return new MessageListenerAdapter(keySpaceNotificationMessageListener);
    }

    @Bean(name = "cacheManager")
    @Primary
    public RedisCacheManager redisCacheManager(JedisConnectionFactory jedisConnectionFactory)
    {
        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .entryTtl(Duration.ofSeconds(10))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.json()));

        redisCacheConfiguration.usePrefix();

        return RedisCacheManager.RedisCacheManagerBuilder.fromConnectionFactory(jedisConnectionFactory)
                .cacheDefaults(redisCacheConfiguration).build();

    }

    @Bean
    ChannelTopic topic() {
        // this should be the one from MinIO notification settings
        ChannelTopic topic = new ChannelTopic(UUID.randomUUID().toString());
        return topic;
    }

    @Bean
    RedisMessageListenerContainer redisMessageListenerContainer() {


        RedisMessageListenerContainer listenerContainer = new RedisMessageListenerContainer();
        listenerContainer.setConnectionFactory(jedisConnectionFactory());
        listenerContainer.addMessageListener(messageListener(), new PatternTopic("__keyspace@*:*"));
        listenerContainer.addMessageListener(messageListener(), new PatternTopic("__keyevent@*:*"));
        listenerContainer.setTaskExecutor(Executors.newFixedThreadPool(4));

        return listenerContainer;
    }

    public void setKeySpaceNotificationMessageListener(KeySpaceNotificationMessageListener keySpaceNotificationMessageListener)
    {
        this.keySpaceNotificationMessageListener = keySpaceNotificationMessageListener;
    }
}
