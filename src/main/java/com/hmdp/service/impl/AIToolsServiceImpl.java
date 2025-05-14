package com.hmdp.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.hmdp.dto.Result;
import com.hmdp.dto.VoucherInfoDTO;
import com.hmdp.entity.Voucher;
import com.hmdp.service.IVoucherService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AIToolsServiceImpl {
    @Resource
    private IVoucherService voucherService;

    public Result findVoucherOpeningWithinHours(int hours) {
        LocalDateTime now = LocalDateTimeUtil.now();
        LocalDateTime deadline = LocalDateTimeUtil.offset(now, hours, ChronoUnit.HOURS);

        List<VoucherInfoDTO> vouchers = voucherService.findVoucherOpeningBefore(deadline);
        return Result.ok(vouchers);
    }

}
