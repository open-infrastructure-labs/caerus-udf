package org.openinfralabs.caerus.eventListenerService.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.openinfralabs.caerus.eventListenerService.sender.RedisSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.annotation.GetMapping;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(SpringExtension.class)
@WebMvcTest(RedisController.class)
class RedisControllerTest {
    @Autowired
    private MockMvc mvc;
    @MockBean
    private RedisSender sender;

    @Test
    void sendDataToRedisQueue() throws Exception {
        Mockito.doNothing().when(this.sender).sendDataToRedisQueue(anyString());
        RequestBuilder request = MockMvcRequestBuilders.get("/redis");

        MvcResult result = mvc.perform(request).andReturn();
        assertEquals("successfully sent", result.getResponse().getContentAsString());
    }
}