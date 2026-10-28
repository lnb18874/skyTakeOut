package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {

    /**
     * 批量新增套餐和菜品的关联关系
     * @param setmealDishes
     */
    void insertBatch(@Param("setmealDishes") List<SetmealDish> setmealDishes);

    /**
     * 根据菜品id集合查询关联关系，用于判断菜品是否被套餐引用
     * @param dishIds
     * @return
     */
    List<SetmealDish> getByDishIds(@Param("dishIds") List<Long> dishIds);

    /**
     * 根据套餐id查询关联的菜品，用于套餐回显
     * @param setmealId
     * @return
     */
    @Select("select * from setmeal_dish where setmeal_id = #{setmealId}")
    List<SetmealDish> getBySetmealId(Long setmealId);

    /**
     * 根据套餐id集合批量删除关联关系
     * @param setmealIds
     */
    void deleteBySetmealIds(@Param("setmealIds") List<Long> setmealIds);
}
