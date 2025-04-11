package com.hmdp.config;


import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RedissionConfig {

    @Bean(name = "redissoonClient1")
    public RedissonClient redissoonClient1(){
        //配置
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        //创建RedissonClient对象
        return Redisson.create(config);
    }
//    @Bean(name = "redissoonClient2")
//    public RedissonClient redissoonClient2(){
//        //配置
//        Config config = new Config();
//        config.useSingleServer().setAddress("redis://127.0.0.1:6380");
//        //创建RedissonClient对象
//        return Redisson.create(config);
//    }
//    @Bean(name = "redissoonClient3")
//    public RedissonClient redissoonClient3(){
//        //配置
//        Config config = new Config();
//        config.useSingleServer().setAddress("redis://127.0.0.1:6381");
//        //创建RedissonClient对象
//        return Redisson.create(config);
//    }
}
