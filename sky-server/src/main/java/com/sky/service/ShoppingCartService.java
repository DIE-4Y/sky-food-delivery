package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService {
    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    void addShoppingCart(ShoppingCartDTO shoppingCartDTO);

    /**
     * 查看购物车数据
     * @return
     */
    List<ShoppingCart> list(Long userId);

    /**
     * 清空购物车
     */
    void cleanAll();

    /**
     * 删除购物车中一个商品
     * @param shoppingCartDTO
     */
    void deleteOne(ShoppingCartDTO shoppingCartDTO);
}
