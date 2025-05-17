package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {
        log.info("正在查询菜品，分类id" + categoryId);
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);

        String key = "dish_" + categoryId;
        //在redis中找
        ValueOperations<String, List<DishVO>> operations = redisTemplate.opsForValue();
        List<DishVO> dishList = operations.get(key);
        if (dishList != null && !dishList.isEmpty()) {
            return Result.success(dishList);
        }

        //查询起售中的菜品
        dish.setStatus(StatusConstant.ENABLE);
        dishList = dishService.listWithFlavor(dish);

        //添加到redis
        operations.set(key, dishList);
        return Result.success(dishList);
    }

}
