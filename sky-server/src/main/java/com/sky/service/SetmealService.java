package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import java.util.List;

public interface SetmealService {

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    List<Setmeal> list(Setmeal setmeal);

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    List<DishItemVO> getDishItemById(Long id);

    /**
     * 新增套餐
     * @param setmealDTO
     * @return
     */
    void saveWithSetmealDish(SetmealDTO setmealDTO);

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 起售停售套餐
     * @param status
     * @param id
     * @return
     */
    void alterStatus(Integer status, Long id);

    /**
     * 根据id查询套餐
     * @param id
     * @return
     */
    SetmealVO getByIdWithSetmealDishes(Long id);

    /**
     * 修改套餐
     * @param setmealDTO
     * @return
     */
    void updateWithSetmealDishes(SetmealDTO setmealDTO);

    /**
     * 批量删除套餐及其相关菜品
     * @param ids
     */
    void deleteWithSetmealDishes(List<Long> ids);
}
