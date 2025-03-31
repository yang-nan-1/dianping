package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;

public class LoginInterceptor implements HandlerInterceptor {


    private RedisTemplate redisTemplate;

    public LoginInterceptor(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        //获取session
//        HttpSession session = request.getSession();
//        //获取session中的用户
//        Object user = session.getAttribute("user");
//        //判断用户是否存在
//        if (user == null){
//            //不存在，拦截
//            response.setStatus(401);
//            return false;
//        }
//        //存在，存储到threadlocal
//         UserHolder.saveUser((UserDTO) user);
//        //放行
//        return true;

        // 获取请求头的 token
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            response.setStatus(401);
            return false;
        }

        // 验证 redisTemplate
        if (redisTemplate == null) {
            throw new RuntimeException("RedisTemplate 或 LOGIN_USER_KEY 未正确初始化");
        }
        // 拼接键并查询 Redis
        String key = LOGIN_USER_KEY + token;
        Map<String, Object> userMap = redisTemplate.opsForHash().entries(key);
        // 防御性检查
        if (userMap == null || userMap.isEmpty()) {
            response.setStatus(401);
            return false;
        }
        // 转换并保存用户信息
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        UserHolder.saveUser(userDTO);
        // 刷新 token 有效期
        redisTemplate.expire(key, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        return true;

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
