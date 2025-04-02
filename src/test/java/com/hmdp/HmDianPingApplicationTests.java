package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;

import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@SpringBootTest(classes = com.hmdp.HmDianPingApplication.class)
public class HmDianPingApplicationTests {
    @Autowired
    private ShopServiceImpl shopService;
    @Resource
    private CacheClient cacheClient;

    @Test
    public void test() {
        shopService.saveShop2Redis(1L, 10L);
    }

    @Test
    public void test1()  throws InterruptedException {
        if(shopService==null)
            System.out.println("shopService is null");
        Shop shop = shopService.getById(1L);
        cacheClient.setWithLogicalExpire(RedisConstants.CACHE_SHOP_KEY + 1L, shop, 10L, TimeUnit.SECONDS);
    }
}
