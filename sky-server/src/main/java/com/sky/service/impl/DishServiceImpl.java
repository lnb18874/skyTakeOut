package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.*;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private SetmealMapper setmealMapper;


    @Override
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        //新增菜品默认起售，避免前端未传status时入库为null
        if (dish.getStatus() == null) {
            dish.setStatus(StatusConstant.ENABLE);
        }
        //向菜品表插入一条数据
        dishMapper.insert(dish);
        Long dishId = dish.getId();
        //向菜品口味表插入多条数据
        saveFlavors(dishDTO.getFlavors(), dishId);
    }

    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 菜品回显：菜品基本信息 + 口味列表
     * @param id
     * @return
     */
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        Dish dish = dishMapper.getById(id);
        if (dish == null) {
            return null;
        }
        List<DishFlavor> flavors = dishFlavorMapper.getByDishId(id);

        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(flavors);
        return dishVO;
    }

    /**
     * 批量删除菜品
     * @param ids
     */
    @Override
    public void deleteBatch(List<Long> ids) {
        //1、校验菜品是否为起售中，起售中的菜品不能删除
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if (dish == null) {
                continue;
            }
            if (StatusConstant.ENABLE.equals(dish.getStatus())) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        //2、校验菜品是否关联了套餐
        List<SetmealDish> setmealDishes = setmealDishMapper.getByDishIds(ids);
        if (setmealDishes != null && setmealDishes.size() > 0) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        //3、删除菜品表及口味表中的数据
        dishMapper.deleteByIds(ids);
        for (Long id : ids) {
            dishFlavorMapper.deleteByDishId(id);
        }
    }

    /**
     * 修改菜品，口味采用先删后插
     * @param dishDTO
     */
    @Override
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.update(dish);

        //口味先全部删除，再重新插入
        dishFlavorMapper.deleteByDishId(dishDTO.getId());
        saveFlavors(dishDTO.getFlavors(), dishDTO.getId());
    }

    /**
     * 根据分类id查询起售的菜品，分类被停售时不返回菜品
     * @param categoryId
     * @return
     */
    @Override
    public List<Dish> list(Long categoryId) {
        List<Dish> dishList = new ArrayList<>();

        Category category = categoryMapper.getById(categoryId);
        if (category == null || StatusConstant.DISABLE.equals(category.getStatus())) {
            //关联的分类已禁用，不展示菜品
            return dishList;
        }

        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);
        dishList = dishMapper.list(dish);
        return dishList;
    }

    @Override
    public void startOrStop(Integer status, Long id) {
        //只在禁售时进行权限校验
        if(StatusConstant.DISABLE.equals(status)){
            List<Setmeal> onSale = setmealMapper.getByDishId(id);
            if(!onSale.isEmpty()){
                //拼接套餐名
                String setmealNames = onSale.stream().map(Setmeal::getName).collect(Collectors.joining("、"));
                throw new SetmealEnableFailedException(MessageConstant.DISH_BE_RELATED_BY_ON_SALE_SETMEAL+":"+setmealNames);
            }
        }
        dishMapper.updateStatus(status, id, BaseContext.getCurrentId());
    }

    /**
     * 保存菜品口味
     * @param flavors
     * @param dishId
     */
    private void saveFlavors(List<DishFlavor> flavors, Long dishId) {
        if (flavors == null || flavors.size() == 0) {
            return;
        }
        flavors.forEach(dishFlavor -> {
            dishFlavor.setDishId(dishId);
        });
        dishFlavorMapper.insertBatch(flavors);
    }
}
