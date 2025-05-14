package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.VoucherInfoDTO;
import com.hmdp.entity.Shop;
import com.hmdp.entity.Voucher;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IShopService;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;

import java.time.LocalDateTime;
import java.util.*;

import static com.hmdp.utils.RedisConstants.SECKILL_STOCK_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 *
 * @since 2021-12-22
 */
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {
    @Resource
    private IShopService shopService;
    @Resource
    private VoucherMapper voucherMapper;

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    
    @Override
    public Result queryVoucherOfShop(Long shopId) {
        // 查询优惠券信息
        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
        // 返回结果
        return Result.ok(vouchers);
    }

    @Override
    @Transactional
    public void addSeckillVoucher(Voucher voucher) {
        // 保存优惠券
        save(voucher);
        // 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherService.save(seckillVoucher);

        //保存库存到redis中
        stringRedisTemplate.opsForValue().set(SECKILL_STOCK_KEY + voucher.getId(),voucher.getStock().toString());
    }

    /**
     * AI用的工作接口
     * @param deadline
     * @return
     */
    @Override
    public List<VoucherInfoDTO> findVoucherOpeningBefore(LocalDateTime deadline) {
        // 1. 查询符合条件的 voucher 列表
        List<Voucher> vouchers = voucherMapper.findVoucherOpeningBefore(deadline);

        if (vouchers == null || vouchers.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 提取所有 shopId
        Set<Long> shopIds = new HashSet<>();
        for (Voucher voucher : vouchers) {
            shopIds.add(voucher.getShopId());
        }

        // 3. 查询店铺信息
        Map<Long, String> shopNameMap = new HashMap<>();
        for (Long shopId : shopIds) {
            Shop shop = shopService.getById(shopId);
            if (shop != null) {
                shopNameMap.put(shopId, shop.getName());
            }
        }

        // 4. 将 Voucher 转换为 VoucherInfoDTO，并填充 shopName
        List<VoucherInfoDTO> result = new ArrayList<>();
        for (Voucher voucher : vouchers) {
            String shopName = shopNameMap.get(voucher.getShopId());
            VoucherInfoDTO dto = VoucherInfoDTO.builder()
                    .shopName(shopName)
                    .title(voucher.getTitle())
                    .subTitle(voucher.getSubTitle())
                    .payValue(voucher.getPayValue())
                    .actualValue(voucher.getActualValue())
                    .type(voucher.getType())
                    .createTime(voucher.getCreateTime())
                    .build();
            result.add(dto);
        }
        return result;
    }

}
