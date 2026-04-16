package com.jiawa.train.business.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/business")
public class RedisController {
    private static final Logger LOG= LoggerFactory.getLogger(RedisController.class);

    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping("/redis/set/{key}/{value}")
    public String set(@PathVariable String key,@PathVariable String value){
        LOG.info("set key: {} value: {}", key, value);
        ValueOperations valueOperations = redisTemplate.opsForValue();
        valueOperations.set(key,value);
        LOG.info("set key:{} value:{}",key,value);
        return "success";
    }

    @GetMapping("/redis/get/{key}")
    public Object get(@PathVariable String key){
        Object object = redisTemplate.opsForValue().get(key);
        LOG.info("get key:{} value:{}",key,object);
        return object;
    }
}
