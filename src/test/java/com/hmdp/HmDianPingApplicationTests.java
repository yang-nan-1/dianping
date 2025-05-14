package com.hmdp;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;

import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisIdWorker;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.junit.jupiter.api.Test;
import org.redisson.api.RCountDownLatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import jakarta.annotation.Resource;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.SHOP_GEO_KEY;

@Data
@SpringBootTest(classes = com.hmdp.HmDianPingApplication.class)
public class HmDianPingApplicationTests {
    @Autowired
    private ShopServiceImpl shopService;
    @Resource
    private CacheClient cacheClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedisIdWorker redisIdWorker;

    private ExecutorService es = Executors.newFixedThreadPool(500);


    //用多线程测试订单号生成
    @Test
    public void testIdWorker() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);
        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisIdWorker.nextId("order");
                System.out.println("id = " + id);
            }
            latch.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            es.submit(task);
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("time = " + (end - begin));
    }
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

    /**
     * 缓存预热，按类别存入店铺信息
     */
    @Test
    void loadShopData(){
        //查询店铺信息
        List<Shop> list = shopService.list();
        //把店铺分组，按照typeId分组，typeId一致的放到一个集合
        Map<Long, List<Shop>> map = list.stream()
                .collect(Collectors.groupingBy(Shop::getTypeId));
        //分批完成写入Redis
        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()){
            //获取类型id
            Long typeId = entry.getKey();
            String key = SHOP_GEO_KEY + typeId;
            //获取同类型的店铺集合
            List<Shop> value = entry.getValue();
            List<RedisGeoCommands.GeoLocation<String>> locations=new ArrayList<>(value.size());

            for (Shop shop : value){
                //写入redis
//                stringRedisTemplate.opsForGeo().add(key,new org.springframework.data.geo.Point(shop.getX(),shop.getY()),shop.getId().toString());
                locations.add(new RedisGeoCommands.GeoLocation<>(
                        shop.getId().toString(),
                        new Point(shop.getX(),shop.getY())
                ));
            }
            stringRedisTemplate.opsForGeo().add(key,locations);
        }
    }

    /**
     * 测试HytyperLogLog
     */
    @Test
    void TestHyperLogLog(){
        String[] values = new String[1000];
        int j=0;
        for (int i = 0; i < 1000000; i++) {
            j = i%1000;
            values[j]="user_"+i;
            if (j==999){
                stringRedisTemplate.opsForHyperLogLog().add("hl2",values);
            }
        }
        //统计数量
        Long count = stringRedisTemplate.opsForHyperLogLog().size("hl2");
        System.out.println("count = "+ count);
    }

    //生成用户数据
    @Test
    public void createData(){
        // 定义输出文件路径（可根据需要修改）
        String filePath = "token_data.txt";

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath))) {
            // 循环生成1-1000的序号和UUID
            for (int i = 1; i <= 1000; i++) {
                // 生成不带连字符的UUID（参数true表示使用基本字母数字）
                String token = UUID.randomUUID().toString(true);
                // 拼接行内容（格式：序号 空格 UUID）
                String line = i + "," + token;
                // 写入行并换行（使用系统默认换行符）
                writer.write(line);
                writer.newLine();
            }
            System.out.println("文件生成成功，路径：" + Paths.get(filePath).toAbsolutePath());
        } catch (IOException e) {
            System.err.println("文件写入失败: " + e.getMessage());
        }
    }


    //预热信息
    @Test
    public void saveUserTokenToRedis() {
        // 定义文件路径（与createData方法生成的文件路径一致）
        String filePath = "token_data.txt";

        try {
            // 读取文件内容
            List<String> lines = Files.readAllLines(Paths.get(filePath));

            // 遍历每一行，将用户信息和token存入Redis
            for (String line : lines) {
                // 按逗号分割，获取userId和token
                String[] parts = line.split(",");
                String userId = parts[0]; // 用户ID
                String token = parts[1];  // Token

                // 将token存入Redis，key为"user:token:userId"，value为token
                String key = LOGIN_USER_KEY + token;
                UserDTO userDTO = UserDTO.builder()
                        .icon("")
                        .nickName("user_" + userId)
                        .id(Long.parseLong(userId))
                        .build();


                Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                        CopyOptions.create().setIgnoreNullValue(true)
                                .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
                stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY+token,userMap);
            }

            System.out.println("用户信息和Token已成功存入Redis！");
        } catch (IOException e) {
            System.err.println("文件读取失败: " + e.getMessage());
        }
    }

}
