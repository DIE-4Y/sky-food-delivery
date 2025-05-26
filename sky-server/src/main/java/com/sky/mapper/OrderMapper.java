package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {
    /**
     * 保存订单信息
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根据条件分页查询订单
     * @param orders
     * @return
     */
    Page<Orders> pageQuery(Orders orders);

    /**
     * 根据id查询订单
     * @param id
     * @return
     */
    @Select("select * from orders where id=#{id}")
    Orders getById(Long id);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 统计各个状态的订单数量
     * @param status
     * @return
     */
    @Select("select count(id) from orders where status=#{status}")
    Integer getCountByStatus(Integer status);

    /**
     * 根据订单状态获取指定时间之前的订单
     * @param status
     * @param orderTime
     * @return
     */
    @Select("select * from orders where status=#{status} and order_time < #{orderTime}")
    List<Orders> getByStatusAndOrderTimeLT(Integer status, LocalDateTime orderTime);

    /**
     * 根据指定时间和状态获取总金额
     * @param beginTime
     * @param endTime
     * @param status
     * @return
     */
    Double sumByTimeAndStatus(LocalDateTime beginTime, LocalDateTime endTime, Integer status);
}
