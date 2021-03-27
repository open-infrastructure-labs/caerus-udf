package org.openinfralabs.caerus.udfService.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.openinfralabs.caerus.openfaasClient.ApiException;
import org.openinfralabs.caerus.openfaasClient.api.DefaultApi;
import org.openinfralabs.caerus.openfaasClient.model.FunctionListEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;



@ExtendWith(SpringExtension.class)
@WebMvcTest(RequestHandler.class)
class RequestHandlerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private DefaultApi defaultApi;

    @MockBean
    private FunctionListEntry functionListEntry;

    @Test
    void handleRequest_serverless() throws Exception {
        try (MockedConstruction<DefaultApi> mocked = Mockito.mockConstruction(DefaultApi.class, (mock, context) -> {
            Mockito.when(mock.systemFunctionFunctionNameGet(anyString())).thenReturn(functionListEntry);
            Mockito.doNothing().when(mock).functionFunctionNamePost(anyString(), any());
        })) {

            RequestBuilder request = MockMvcRequestBuilders.get("/testBucket/testObj");

            MvcResult result = mvc.perform(request).andReturn();
            assertEquals("Udf invoked successfully.", result.getResponse().getContentAsString());
            assertEquals(200, result.getResponse().getStatus());
        }

    }
}

