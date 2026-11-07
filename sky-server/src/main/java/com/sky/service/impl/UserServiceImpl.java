package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    private final WeChatProperties weChatProperties;
    private static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";
    @Autowired
    private UserMapper userMapper;

    public UserServiceImpl(WeChatProperties weChatProperties) {
        this.weChatProperties = weChatProperties;
    }

    /**
     * 微信登录
     * @param userLoginDTO
     * @return
     */
    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {
        //调用接口，获取openid
        String openId = getOpenId(userLoginDTO.getCode());
        //判断openid是否为空，如果为空则说明登录失败
        if(openId==null){
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        //判断是否为新用户
        User user = userMapper.getByOpenId(openId);

        //如果是新用户，则需要将用户信息保存到数据库
        if(user==null){
            user = User.builder()
                    .openid(openId)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }

        //返回用户对象

        return user;
    }

    /**
     * 根据code获取openId
     * @param code
     * @return
     */
    private String getOpenId(String code) {
        Map<String,String> map = new HashMap<>();
        map.put("appid",weChatProperties.getAppid());
        map.put("secret",weChatProperties.getSecret());
        map.put("js_code",code);
        map.put("grant_type","authorization_code");
        String json= HttpClientUtil.doGet(WX_LOGIN,map);

        JSONObject jsonObject = JSON.parseObject(json);
        String openId = jsonObject.getString("openid");
        return openId;
    }
}
