package com.hmdp.consumer;

import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.IVoucherOrderService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@RocketMQMessageListener(topic = "seckill_order_topic",
        consumerGroup = "seckill_order_group")
public class SeckillOrderConsumer implements RocketMQListener<VoucherOrder> {

    @Resource
    private IVoucherOrderService voucherOrderService;
    //TODO: 准备好10000个可以抢购的用户，测试的时候直接跑一万以上的高并发看看是否会出问题
    @Override
    public void onMessage(VoucherOrder voucherOrder) {
        // 处理订单
        voucherOrderService.createVoucherOrder(voucherOrder);
    }
}