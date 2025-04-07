package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Value("${spring.mail.username}")
    private String from;
    @Resource
    private JavaMailSender javaMailSender;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private UserMapper userMapper;
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1校验手机号
        if (RegexUtils.isPhoneInvalid(phone)){
            //2不符合返回错误信息
            return Result.fail("手机号格式错误");
        }
        //3符合、生成验证码
        String code = RandomUtil.randomNumbers(6);
//        //4.保存验证码到session
//        session.setAttribute("code",code);
        //保存到redis
        redisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //5发送验证码,这里需要第三方平台所以不细致去做了
        log.debug("发送验证码成功,验证码:{}",code);

        /**
        // 构建一个邮件对象
        SimpleMailMessage message = new SimpleMailMessage();
        // 设置邮件发送者
        message.setFrom(from);
        // 设置邮件接收者
        message.setTo("1962155093@qq.com");
        // 设置邮件的主题
        message.setSubject("登录验证码");
        // 设置邮件的正文
        String text = "您的验证码为：" + code + ",请勿泄露给他人。";
        message.setText(text);
        // 发送邮件
        try {
            javaMailSender.send(message);
            log.debug("发送成功{}",code);
        } catch (MailException e) {
            e.printStackTrace();
            log.debug( "发送失败");
        }**/
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)){
            //不符合返回错误信息
            return Result.fail("手机号格式错误");
        }

//        //2校验验证码
//        Object cacheCode = session.getAttribute("code");
//        String code = loginForm.getCode();
//        if(cacheCode==null||!cacheCode.toString().equals(code)){
//            //3不一致报错
//            return Result.fail("验证码错误");
//        }

        //验证码校验
        Object cacheCode = redisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
        if(cacheCode==null||!cacheCode.toString().equals(code)){
            //3不一致报错
            return Result.fail("验证码错误");
        }
        //4一致，根据手机号查询用户
        User user = query().eq("phone", phone).one();
        //5判断用户是否存在
        if (user==null){
            //6不存在创建新用户并保存
            user = createUserWithPhone(phone);
        }
//        //7保存用户到session中
//        session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));

        //7保存用户到redis中
        //7.1生成随机token，作为登录令牌
        String token = UUID.randomUUID().toString(true);
        //7.2将User对象转为HashMap存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO);
        //7.3存储
        redisTemplate.opsForHash().putAll(LOGIN_USER_KEY+token,userMap);
        //7.4设置token有效期
        redisTemplate.expire(LOGIN_USER_KEY+token,LOGIN_USER_TTL,TimeUnit.MINUTES);
        //8返回token
        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {
        User user= User.builder()
                .phone(phone)
                .nickName(USER_NICK_NAME_PREFIX + RandomUtil.randomNumbers(10))
                .build();
        save(user);
        return user;
    }
}
