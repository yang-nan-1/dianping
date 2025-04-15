package com.hmdp.controller;


import com.hmdp.service.IFollowService;
import com.sun.xml.internal.ws.developer.MemberSubmissionAddressing;
import org.springframework.web.bind.annotation.*;
import com.hmdp.dto.Result;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 */
@RestController
@RequestMapping("/follow")
public class FollowController {

    @Resource
    private IFollowService followService;

    @PutMapping("/{Id}/{isFollow}")
    public Result follow(@PathVariable("Id") Long followId, @PathVariable("isFollow") Boolean isFollow){
        return followService.follow(followId,isFollow);
    }

    @GetMapping("/or/not/{Id}")
    public Result isFollow(@PathVariable("Id") Long followId){
        return followService.isFollow(followId);
    }
}
