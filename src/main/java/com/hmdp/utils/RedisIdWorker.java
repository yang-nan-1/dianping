package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;

@Component
public class RedisIdWorker {
    /**
     * 开始时间戳
     * 2525-1-1
     */
    private static final long BEGIN_TIMESTAMP = 1735689600L;

    /**
     *序列号位数
     */
    private static final int COUNT_BITS = 32;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public long  nextId(String keyPrefix){
        //1.生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(java.time.ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;
        //2.生成序列号
        //2.1获取当天日期
        String date = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        //2。2自增长
        long count = stringRedisTemplate.opsForValue().increment("icr:"+keyPrefix+":"+date);
        //3.拼接并返回
        return timestamp<< COUNT_BITS | count; //符号位+31位时间戳+32位序列号
    }

//    public static void main(String[] args) {
//        //获取开始时间戳
//        LocalDateTime time = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
//        long l = time.toEpochSecond(java.time.ZoneOffset.UTC);
//        System.out.println(l);
//    }
}
