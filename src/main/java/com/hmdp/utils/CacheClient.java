package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;

@Component
@Slf4j
public class CacheClient {
    public final StringRedisTemplate stringRedisTemplate;
    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        //封装逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        //写入redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    public <R,ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID,R> dbFallback, Long time, TimeUnit unit) {
        String key = CACHE_SHOP_KEY + id;
        //查询商铺缓存
        String shopJson = (String) stringRedisTemplate.opsForValue().get(key);
        //判断是否存在
        if (StrUtil.isNotBlank(shopJson)){
            //存在直接返回
            return JSONUtil.toBean(shopJson, type);
        }
        //判断命中是否为空值
        if (shopJson!=null) //null和""是两个概念，这里判断是否为"",如果是null证明其实是由这个数据的可以继续查寻，如果是""证明缓存穿透（数据库无数据）直接终止
            return null;

        //不存在根据id查询数据库
        R r = dbFallback.apply(id);
        //数据库不存在，返回错误信息
        if (r==null){
            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }

        //存在，写入redis，返回数据
        this.set(key,r,time,unit);
        return r;
    }


    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }

    public static final ExecutorService CACHE_REBULID_EXECUTOR = Executors.newFixedThreadPool(10);
    public <R,ID> R queryWithLogicalExpire(String keyPrefix,ID id,Class<R> type,Function<ID,R> dbfallback,Long time,TimeUnit unit){
        String key = CACHE_SHOP_KEY + id;
        //查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //判断是否存在
        if (StrUtil.isBlank(shopJson)){
            //不存在直接返回null
            return null;
        }
        //命中，jasn反序列化
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        //未过期返回店铺信息
        if (expireTime.isAfter(LocalDateTime.now()))
            return r;
        //已过期，需要重建缓存
        //缓存重建
        //获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        if (isLock) {
            // 双重检测
            if (expireTime.isAfter(LocalDateTime.now())) {
                return r;
            }
            // 启动异步任务重建缓存
            CACHE_REBULID_EXECUTOR.submit(() -> {
                try {
                    R shop = dbfallback.apply(id);
                    this.setWithLogicalExpire(key, shop, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unLock(lockKey);
                }
            });
        }
        //失败，返回过期商户信息
        return r;
    }
}
