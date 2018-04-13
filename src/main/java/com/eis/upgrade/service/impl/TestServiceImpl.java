package com.eis.upgrade.service.impl;

import org.springframework.stereotype.Service;

/**
 * @author 潘峰
 * @date 12/04/2018 5:05 PM
 */
@Service
public class TestServiceImpl implements TestService {
    @Override
    public String hello() {
        return "hello";
    }

}
