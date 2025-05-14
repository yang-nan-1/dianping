package com.hmdp.consumer;

import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.IVoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;


@Slf4j
@Service
@RocketMQMessageListener(topic = "seckill_order_topic",
        consumerGroup = "seckill_order_group")
public class SeckillOrderConsumer implements RocketMQListener<VoucherOrder> {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private IVoucherOrderService voucherOrderService;

    @Resource
    private ThreadPoolTaskExecutor taskExecutor;

    @Override
    public void onMessage(VoucherOrder voucherOrder) {
        taskExecutor.execute(() -> {
            Long userId = voucherOrder.getUserId();
            // 创建锁对象
            RLock lock = redissonClient.getLock("lock:order:" + userId);

            // 获取锁
            boolean isLock = lock.tryLock();
            // 判断是否获取锁成功
            if (!isLock) {
                log.error("获取锁失败");
                return;
            }
            try {
                // 处理订单
                voucherOrderService.createVoucherOrder(voucherOrder);
            } finally {
                // 释放锁
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        });
    }
}