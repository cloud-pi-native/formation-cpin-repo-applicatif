package fr.gouv.interieur.dso.controller;

import fr.gouv.interieur.dso.models.Demo;
import fr.gouv.interieur.dso.service.jdbc.DemoService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.gouv.interieur.dso.service.dto.ResponseDto;

import java.util.List;

@RestController
@RequestMapping("/api/demo")
public class DemoController {

	Logger logger = LoggerFactory.getLogger(DemoController.class);
	
    @Autowired
    private DemoService demoService;

    @GetMapping("/")
    public ResponseDto helloWorld() {
        ResponseDto responseDto = new ResponseDto();
        responseDto.setMessage("hello world !");
        responseDto.setStatus("ok");
        return responseDto;
    }

    @GetMapping("/demo")
    public ResponseEntity<List<Demo>> getDemosList(){
    	logger.info("Get Demo API");
        return new ResponseEntity<>(demoService.getListDemo(), HttpStatus.OK);
    }
}
