package org.openinfralabs.caerus.UdfRegistry.controller;

import org.openinfralabs.caerus.UdfRegistry.model.NewUdf;
import org.openinfralabs.caerus.UdfRegistry.model.Udf;
import org.openinfralabs.caerus.UdfRegistry.service.UdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.annotations.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
//@RequestMapping("/api")
public class UdfController {

    Logger logger = LoggerFactory.getLogger(UdfController.class);
    @Autowired
    private UdfService udfService;

    /**
     *
     * @param newUdf
     * @param file
     * @return
     */
    // When send both binary file and json object, we can only use form, not raw data request body, so the newUdf can only be string, not object
    // so send string all the way to udfDaoImpl to change back to NewUdf object from string explicitly
    @PostMapping(value="/udf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiOperation(value = "Create a new udf and save to the UDF Registry, input form with a json config and a MultipartFile",
            notes = "Send via an input form with a json config and a MultipartFile. The json config must include followings:" +
                    "  private String name;\n" +
                    "  private int pkg; //0 = function; 1 = docker\n" +
                    "  private String language;\n" +
                    "  private BigDecimal interfaceVersion;\n" +
                    "  private BigDecimal languageVersion;\n" +
                    "  private String main;")
    public ResponseEntity<String> saveUdf(@RequestParam("newUdfJson") String newUdf, @RequestParam("uploadFile") MultipartFile file) {
        boolean result = udfService.saveUdf(newUdf, file);
        if (result) {
            logger.info("UDF uploaded successfully.");
            return ResponseEntity.ok("Udf uploaded successfully.");
        } else {
            logger.error("Failed to upload UDF.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    
    @GetMapping("/udf")
    @ApiOperation(value = "Fetch all UDfs' configuration information from the registry.",
                notes = "Fetch all Udfs configuration in json format.")
    public ResponseEntity<List<Udf>> fetchAllUdf() {
        List<Udf> listUdfs = udfService.fetchAllUdf();
        return ResponseEntity.ok(listUdfs);
    }


    @GetMapping("/udf/{id}")
    @ApiOperation(value = "Fetch an Udf via Id.",
            notes = "Input is an UDF Id.")
    public ResponseEntity<Udf> getUdfbyId(@PathVariable("id") String id) {
        Udf udf = udfService.getUdfById(id);
        return ResponseEntity.ok(udf);
    }

    @DeleteMapping("/udf/{id}")
    @ApiOperation(value = "Delete an Udf via Id.",
            notes = "Input is an UDF Id.")
    public ResponseEntity<String> deleteUdfbyId(@PathVariable("id") String id) {
        boolean result = udfService.deleteUdfById(id);
        if (result)
            return ResponseEntity.ok("Udf deleted successfully.");
        else
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    @GetMapping("/udf/{id}/executable")
    @ApiOperation(value = "Fetch the UDF image/file via Id.",
            notes = "Input is an UDF Id.")
    public ResponseEntity<Resource> getFileById(@PathVariable("id") String id) {
        Resource fileSystemResource = udfService.getFileResourceById(id);
        return ResponseEntity.ok(fileSystemResource);
    }


    @PutMapping("/udf/{id}")
    @ApiOperation(value = "Update the UDF configuration.",
            notes = "Input is an UDF Id and a full or partial UDF config string in json format.")
    public ResponseEntity<String> updateUdfConfig(@PathVariable("id") String id, @RequestParam("udfConfig") String updateUdfConfig) {
        boolean result = udfService.updateUdf(id, updateUdfConfig);
        if (result)
            return ResponseEntity.ok("Udf updated successfully.");
        else
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }


    @PutMapping("/udf/{id}/updateExecutable")
    @ApiOperation(value = "Update the UDF image/file.",
            notes = "Input is an UDF Id and an image/file of the UDF executable.")
    public ResponseEntity<String> updateUdfExecutable(@PathVariable("id") String id, @RequestParam("uploadFile") MultipartFile file) {
        boolean result = udfService.updateUdfExecutable(id, file);
        if (result)
            return ResponseEntity.ok("Udf executable updated successfully.");
        else
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

}
