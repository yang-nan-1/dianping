package com.hmdp.controller;

import com.hmdp.service.IFollowService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import com.hmdp.dto.Result;



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

    @GetMapping("/common/{id}")
    public Result followCommons(@PathVariable("id") Long id){
        return followService.followCommons(id);
    }
}
