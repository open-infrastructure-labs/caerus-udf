package org.openinfralabs.caerus.ndp.udf.faas;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.Valid;
import function.Handler;

@Controller
public class HomeHandler {

    @Autowired
    private Handler handler;

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public ResponseEntity<String> handle(@RequestBody @Valid byte[] payload) {
        String response = handler.handle(payload);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
