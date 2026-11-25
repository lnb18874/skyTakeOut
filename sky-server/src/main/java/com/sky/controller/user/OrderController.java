package com.sky.controller.user;

import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController("userOrderController")
@RequestMapping("/user/order")
public class OrderController {

    @Autowired
    private OrderService orderService;
    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @PostMapping("/submit")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO){
        OrderSubmitVO orderSubmitVO = orderService.submitOrder(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);
        return Result.success(orderPaymentVO);
    }

    /**
     * 分页查询历史订单
     * @param page
     * @param pageSize
     * @param status 订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
     * @return
     */
    @GetMapping("historyOrders")
    public Result<PageResult> page(int page, int pageSize, Integer status){
        log.info("分页查询历史订单：{}", page);
        PageResult pageResult = orderService.pageQuery4User(page, pageSize, status);
        return Result.success(pageResult);
    }

    /**
     * 查询订单详情
     * @param orderId
     * @return
     */
    @GetMapping("/orderDetail/{id}")
    public Result<OrderVO> details(@PathVariable("id") Long orderId){
        OrderVO orderVO = orderService.getOrderDetail(orderId);
        log.info("查询订单详情：{}", orderVO);
        return Result.success(orderVO);
    }

    /**
     * 取消订单
     * @param orderId
     * @return
     */
    @PutMapping("/cancel/{id}")
    public Result cancel(@PathVariable("id") Long orderId){
        orderService.cancel(orderId);
        return Result.success();
    }

    /**
     * 再来一单
     * @param id
     * @return
     */
    @PostMapping("/repetition/{id}")
    public Result repetition(@PathVariable("id") Long id){
        orderService.repetitionOrder(id);
        return Result.success();
    }

    @GetMapping("/reminder/{id}")
    public Result reminder(@PathVariable("id") Long id){
        log.info("再来一单：{}", id);
        orderService.reminder(id);
        return Result.success();
    }
}
