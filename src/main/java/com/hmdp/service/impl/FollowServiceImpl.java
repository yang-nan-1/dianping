package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Override
    public Result follow(Long followId, Boolean isFollow) {
       //获取登录用户
        Long userId = UserHolder.getUser().getId();
        //判断是关注还是取关
        if(isFollow){
            //关注，新增数据
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followId);
            save(follow);
        }
        else {
            //取关，删除数据
            remove(new QueryWrapper<Follow>()
                    .eq("user_id",userId).eq("follow_user_id",followId));
        }
        return Result.ok();
    }

    @Override
    public Result isFollow(Long followId) {
        Long userId = UserHolder.getUser().getId();
        Integer count = query().eq("user_id", userId).eq("follow_user_id", followId).count();
        return Result.ok(count>0);
    }
}
