package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 处理支付超时订单
     */
    @Scheduled(cron = "0 * * * * ?")//每分钟自动处理
    public void processTimeOutOrder(){
        log.info("正在处理支付超时订单");
        //获取超时订单
        LocalDateTime lt = LocalDateTime.now().plusMinutes(-15);
        LocalDateTime now = LocalDateTime.now();
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, lt);
        //修改订单状态
        if(ordersList != null && !ordersList.isEmpty()){
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单支付超时");
                orders.setCancelTime(now);
                orderMapper.update(orders);
            }
        }
    }

    /**
     * 处理配送超时订单
     */
    @Scheduled(cron = "0 0 1 * * ?")//每天1点自动处理前一天
    public void processDeliveryOder(){
        log.info("定时处理派送中的订单");
        LocalDateTime lt = LocalDateTime.now().plusHours(-1);
        List<Orders> ordersList = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, lt);
        //修改订单状态
        if(ordersList != null && !ordersList.isEmpty()){
            for (Orders orders : ordersList) {
                orders.setStatus(Orders.COMPLETED);
                orderMapper.update(orders);
            }
        }
    }
}
