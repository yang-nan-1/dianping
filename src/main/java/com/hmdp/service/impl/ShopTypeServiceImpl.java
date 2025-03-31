package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.sql.Struct;
import java.util.List;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    String key = CACHE_SHOP_TYPE;
    @Override
    public Result showQuery() {
        //查询缓存
        String shopType = stringRedisTemplate.opsForValue().get(key);
        //判断是否存在
        if (StrUtil.isNotBlank(shopType)) {
            List<ShopType> shopTypeList = JSONUtil.toList(shopType, ShopType.class);
            //存在直接返回
            return Result.ok(shopTypeList);
        }
        //不存在去数据库查询
        List<ShopType> shopTypes = query().orderByAsc("sort").list();

        //不存在返回报错
        if (shopTypes==null)
            return Result.fail("不存在店铺类型!");
        //存在写入缓存
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shopTypes));
        return Result.ok(shopTypes);
    }
}
