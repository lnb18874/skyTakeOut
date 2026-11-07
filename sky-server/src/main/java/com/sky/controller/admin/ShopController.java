package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequestMapping("admin/shop")
@RestController("adminShopController")
public class ShopController {

    public static final String SHOP_STATUS = "ShopStatus";

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 修改店铺状态
     * @param status
     * @return
     */
    @PutMapping("/{status}")
    public Result setStatus(@PathVariable Integer status){
        log.info("修改店铺状态为：{}", status==1 ? "营业中" : "打烊中");
        redisTemplate.opsForValue().set(SHOP_STATUS, status);
        return Result.success();
    }

    /**
     * 获取店铺状态
     * @return
     */
    @GetMapping("/status")
    public Result<Integer> getStatus(){
        Integer status = (Integer) redisTemplate.opsForValue().get(SHOP_STATUS);
        log.info("获取店铺状态为：{}", status==1 ? "营业中" : "打烊中");
        return Result.success(status);
    }
}
