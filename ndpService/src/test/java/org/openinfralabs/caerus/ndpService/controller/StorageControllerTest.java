package org.openinfralabs.caerus.ndpService.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.openinfralabs.caerus.ndpService.service.StorageAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.BDDMockito.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(StorageController.class)
class StorageControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private StorageAdapter adapter;

    @Test
    void listObjects() throws Exception {

        Mockito.when(this.adapter.listObjects(anyString(), any())).thenReturn(true);
        RequestBuilder request = MockMvcRequestBuilders.get("/testBucket/");

        MvcResult result = mvc.perform(request).andReturn();
        assertEquals(200, result.getResponse().getStatus());

    }

}