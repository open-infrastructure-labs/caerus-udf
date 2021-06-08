package org.openinfralabs.caerus.ndpService.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.openinfralabs.caerus.ndpService.service.StorageAdapter;
import org.openinfralabs.caerus.ndpService.service.StorageAdapterHdfsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyString;

@ExtendWith(SpringExtension.class)
@WebMvcTest(HDFSStorageController.class)
class StorageControllerHdfsTest {

    @Autowired
    private MockMvc mvc;

    //@MockBean(name = "storageAdapterMinioImpl")
    //private StorageAdapter adapterMinio;

    @MockBean(name = "storageAdapterHdfsImpl")
    private StorageAdapterHdfsImpl adapterHdfs;

    @Test
    void listObjects() throws Exception {
        //TODO
/*
        Mockito.when(this.adapterHdfs.listObjects(anyString(), any())).thenReturn(true);
        RequestBuilder request = MockMvcRequestBuilders.get("/testBucket/");

        MvcResult result = mvc.perform(request).andReturn();
        assertEquals(200, result.getResponse().getStatus());*/

    }

}