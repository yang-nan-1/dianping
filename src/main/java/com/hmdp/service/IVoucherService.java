package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.dto.VoucherInfoDTO;
import com.hmdp.entity.Voucher;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 *
 * @since 2021-12-22
 */
public interface IVoucherService extends IService<Voucher> {

    Result queryVoucherOfShop(Long shopId);

    void addSeckillVoucher(Voucher voucher);

    List<VoucherInfoDTO> findVoucherOpeningBefore(LocalDateTime deadline);
}
