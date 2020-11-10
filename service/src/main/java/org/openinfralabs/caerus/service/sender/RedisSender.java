package org.openinfralabs.caerus.service.sender;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

// this class is for test only, in normal case, only receiver is needed
@Service
public class RedisSender {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ChannelTopic topic;

    public void sendDataToRedisQueue(String input) {
        redisTemplate.convertAndSend(topic.getTopic(), input);
        System.out.println("Data -" + input + " sent via Redis topic - " + topic.getTopic());
    }

}
