package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 优惠券信息封装类，包含店铺名
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VoucherInfoDTO {
    private String shopName;      // 店铺名称
    private String title;         // 优惠券标题
    private String subTitle;      // 副标题
    private Long payValue;       // 支付金额（单位分）
    private Long actualValue;     // 抵扣金额（单位分）
    private Integer type;         // 优惠券类型（0普通券，1秒杀券）
    private LocalDateTime createTime; // 创建时间
}
