package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 套餐业务层
 */
@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;

    /**
     * 新增套餐，同时保存套餐和菜品的关联关系
     * @param setmealDTO
     */
    public void saveWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        //新增套餐默认停售，套餐内可能有未起售的菜品
        if (setmeal.getStatus() == null) {
            setmeal.setStatus(StatusConstant.DISABLE);
        }

        //向套餐表插入一条数据
        setmealMapper.insert(setmeal);
        Long setmealId = setmeal.getId();

        //向套餐菜品关系表插入多条数据
        saveSetmealDishes(setmealDTO.getSetmealDishes(), setmealId);

        //起售状态需要校验关联菜品是否全部起售
        if (StatusConstant.ENABLE.equals(setmeal.getStatus())) {
            checkDishOnSale(setmealId);
        }
    }

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 套餐回显：套餐基本信息 + 关联的菜品列表
     * @param id
     * @return
     */
    public SetmealVO getByIdWithDish(Long id) {
        Setmeal setmeal = setmealMapper.getById(id);
        if (setmeal == null) {
            return null;
        }
        List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);

        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }

    /**
     * 批量删除套餐
     * @param ids
     */
    public void deleteBatch(List<Long> ids) {
        //1、校验套餐是否为起售中，起售中的套餐不能删除
        for (Long id : ids) {
            Setmeal setmeal = setmealMapper.getById(id);
            if (setmeal == null) {
                continue;
            }
            if (StatusConstant.ENABLE.equals(setmeal.getStatus())) {
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }

        //2、删除套餐表及套餐菜品关系表中的数据
        setmealMapper.deleteByIds(ids);
        setmealDishMapper.deleteBySetmealIds(ids);
    }

    /**
     * 修改套餐，关联菜品采用先删后插
     * @param setmealDTO
     */
    public void updateSetmeal(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.update(setmeal);

        Long setmealId = setmealDTO.getId();
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        //传了菜品列表才重建关联关系，先删除再插入，避免未传该字段时误删已有的关联菜品
        if (setmealDishes != null && setmealDishes.size() > 0) {
            setmealDishMapper.deleteBySetmealIds(Collections.singletonList(setmealId));
            saveSetmealDishes(setmealDishes, setmealId);
        }

        //起售状态需要校验关联菜品是否全部起售
        if (StatusConstant.ENABLE.equals(setmeal.getStatus())) {
            checkDishOnSale(setmealId);
        }
    }

    /**
     * 套餐起售、停售
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {
        //起售套餐时，套餐内包含未起售菜品则不允许起售
        if (StatusConstant.ENABLE.equals(status)) {
            checkDishOnSale(id);
        }

        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setmealMapper.update(setmeal);
    }

    /**
     * 条件查询套餐
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        return setmealMapper.list(setmeal);
    }

    /**
     * 保存套餐和菜品的关联关系
     * @param setmealDishes
     * @param setmealId
     */
    private void saveSetmealDishes(List<SetmealDish> setmealDishes, Long setmealId) {
        if (setmealDishes == null || setmealDishes.size() == 0) {
            return;
        }
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealId);
        });
        setmealDishMapper.insertBatch(setmealDishes);
    }

    /**
     * 校验套餐关联的菜品是否全部处于起售状态
     * @param setmealId
     */
    private void checkDishOnSale(Long setmealId) {
        List<Dish> dishList = dishMapper.getBySetmealId(setmealId);
        if (dishList == null || dishList.size() == 0) {
            //套餐未关联菜品，无法起售
            throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
        }
        dishList.forEach(dish -> {
            if (StatusConstant.DISABLE.equals(dish.getStatus())) {
                throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
            }
        });
    }
}
