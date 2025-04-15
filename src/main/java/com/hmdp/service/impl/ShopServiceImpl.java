package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 */
@Service()
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    public ShopServiceImpl() {
        System.out.println("ShopServiceImpl is instantiated");
    }

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    public static final ExecutorService CACHE_REBULID_EXECUTOR = Executors.newFixedThreadPool(10);
    @Override
    public Result queryById(Long id) {
        //缓存穿透
//        Shop shop = queryWithPassThrough(id);

        //互斥锁解决缓存击穿
        Shop shop = queryWithMutex(id);

        //逻辑过期解决缓存击穿
//        Shop shop = queryWithLogicalExpire(id);

        //用工具类解决缓存穿透
//        Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY, id,
//                Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //缓存击穿
//        Shop shop = cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY, id,
//                Shop.class, this::getById, 20L, TimeUnit.SECONDS);
        if (shop == null) {
            return Result.fail("店铺不存在！");
        }
        return Result.ok(shop);
    }
    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }


    //解决缓存穿透问题
//    public Shop queryWithPassThrough(Long id){
//        String key = CACHE_SHOP_KEY + id;
//        //查询商铺缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        //判断是否存在
//        if (StrUtil.isNotBlank(shopJson)){
//            //存在直接返回
//            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
//            return shop;
//        }
//        //判断命中是否为空值
//        if (shopJson!=null) //null和""是两个概念，这里判断是否为"",如果是null证明其实是由这个数据的可以继续查寻，如果是""证明缓存穿透（数据库无数据）直接终止
//            return null;
//
//        //不存在根据id查询数据库
//        Shop shop = getById(id);
//        //数据库不存在，返回错误信息
//        if (shop==null){
//            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
//            return null;
//        }
//
//        //存在，写入redis，返回数据
//        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        return shop;
//    }


    //解决缓存击穿问题
    public Shop queryWithMutex(Long id){
        String key = CACHE_SHOP_KEY + id;
        //查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //判断是否存在
        if (StrUtil.isNotBlank(shopJson)){
            //存在直接返回
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }
        //判断命中是否为空值
        if (shopJson!=null) //null和""是两个概念，这里判断是否为"",如果是null证明其实是由这个数据的可以继续查寻，如果是""证明缓存穿透（数据库无数据）直接终止
            return null;


        //实现缓存重建
        String lockKey = LOCK_SHOP_KEY + id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
            if (!isLock){
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            //成功获取锁
            shop = getById(id);
            //模拟重建延迟
            Thread.sleep(200);
            //数据库不存在，返回错误信息
            if (shop==null){
                stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            //存在，写入redis，返回数据
            stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            unLock(lockKey);
        }
        return shop;
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id==null)
            return Result.fail("店铺id不能为空!");
        //先对数据库进行更新
        updateById(shop);
        //删除缓存
        String key = CACHE_SHOP_KEY + id;
        stringRedisTemplate.delete(key);
        return Result.ok();
    }

    //    public Shop queryWithLogicalExpire(Long id){
//        String key = CACHE_SHOP_KEY + id;
//        //查询商铺缓存
//        String shopJson = (String) redisTemplate.opsForValue().get(key);
//        //判断是否存在
//        if (StrUtil.isBlank(shopJson)){
//            //不存在直接返回null
//            return null;
//        }
//        //命中，jasn反序列化
//        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
//        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
//        LocalDateTime expireTime = redisData.getExpireTime();
//        //未过期返回店铺信息
//        if (expireTime.isAfter(LocalDateTime.now()))
//            return shop;
//        //已过期，需要重建缓存
//        //缓存重建
//        //获取互斥锁
//        String lockKey = LOCK_SHOP_KEY + id;
//        boolean isLock = tryLock(lockKey);
//        if (isLock) {
//            // 双重检测
//            if (expireTime.isAfter(LocalDateTime.now())) {
//                return shop;
//            }
//            // 启动异步任务重建缓存
//            CACHE_REBULID_EXECUTOR.submit(() -> {
//                try {
//                    this.saveShop2Redis(id, 10L);
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                } finally {
//                    unLock(lockKey);
//                }
//            });
//        }
//        //失败，返回过期商户信息
//        return shop;
//    }
//
//
    public void saveShop2Redis(Long id,Long expireSec){
        //查询店铺数据
        Shop shop = getById(id);
        //封装逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSec));
        //写入redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(redisData));
    }
}
