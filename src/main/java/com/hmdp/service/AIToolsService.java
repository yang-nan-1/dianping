package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.service.impl.AIToolsServiceImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class AIToolsService {
    @Resource
    private AIToolsServiceImpl aiTools;

    @Tool("查询未来一定时间内开放获取的优惠券有哪些")
    public Result findVoucherOpeningWithinDays(
            @P("小时数") int hours,
            @P("天数") int days) {

        // 如果用户只提供了天数，则转为小时
        if (hours == 0 && days > 0) {
            hours = days * 24;
        }
        return aiTools.findVoucherOpeningWithinHours(hours);
    }


}
