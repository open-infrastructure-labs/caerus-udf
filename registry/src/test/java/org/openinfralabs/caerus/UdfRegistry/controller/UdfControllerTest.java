package org.openinfralabs.caerus.UdfRegistry.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.openinfralabs.caerus.UdfRegistry.service.UdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(SpringExtension.class)
@WebMvcTest(UdfController.class)
class UdfControllerTest {
    @Autowired
    private MockMvc mvc;
    @MockBean
    private UdfService udfService;

    @Test
    void saveUdf_simple_test() throws Exception {
        Mockito.when(this.udfService.saveUdf(anyString(), any())).thenReturn(true);

        MockMultipartFile firstFile = new MockMultipartFile("uploadFile", "filename.csv", "text/plain", "some CSV data".getBytes());
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.multipart("/udf").file(firstFile).param("newUdfJson", "newudf");

        MvcResult result = mvc.perform(request).andReturn();
        assertEquals("Udf uploaded successfully.", result.getResponse().getContentAsString());
        assertEquals(200, result.getResponse().getStatus());
    }

    @Test
    void fetchAllUdf() {
    }

    @Test
    void getUdfbyId() {
    }

    @Test
    void deleteUdfbyId() {
    }

    @Test
    void updateUdfExecutable() {
    }


    @Test
    void updateUdfConfig() {
    }

    @Test
    void getFileById() {
    }
}
