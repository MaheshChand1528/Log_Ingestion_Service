package com.fable.assignment.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.fable.assignment.model.LogData;
import com.fable.assignment.services.LogService;

@RestController
public class LogController {
	
	@Autowired
    private LogService logService;

    @PostMapping("/log")
    public ResponseEntity<String> receiveLog(@RequestBody LogData log) {
        logService.bufferLog(log);
        return new ResponseEntity<>("Log received", HttpStatus.OK);
    }

}
