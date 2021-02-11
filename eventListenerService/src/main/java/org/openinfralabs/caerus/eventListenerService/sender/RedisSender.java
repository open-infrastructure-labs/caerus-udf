package org.openinfralabs.caerus.eventListenerService.sender;

import org.openinfralabs.caerus.eventListenerService.receiver.KeySpaceNotificationMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

// this class is for test only, in normal case, only receiver is needed
@Service
public class RedisSender {
    Logger logger = LoggerFactory.getLogger(KeySpaceNotificationMessageListener.class);

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ChannelTopic topic;

    public void sendDataToRedisQueue(String input) {
        // this is for test only, the live code path will be the KeySapceNotificationMessageListener to listen the event
        redisTemplate.convertAndSend(topic.getTopic(), input);
        logger.info("Data -" + input + " sent via Redis topic - " + topic.getTopic());
    }

}
