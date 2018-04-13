package com.eis.upgrade.controller;

import com.eis.upgrade.service.impl.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 潘峰
 * @date 12/04/2018 3:54 PM
 */
@RestController
@RequestMapping("/test")
public class TestController {


    @Autowired
    private TestService testService;

    @GetMapping("/hello")
    public String hello() {
        return testService.hello();
    }


}
