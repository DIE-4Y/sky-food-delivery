package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper {
    /**
     * 保存订单信息
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根据条件查询所有订单
     * @param orders
     * @return
     */
    Page<Orders> list(Orders orders);
}
