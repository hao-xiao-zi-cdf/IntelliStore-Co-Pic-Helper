package com.hao_xiao_zi.intellistorecopichelper.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.space.*;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.Picture;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.User;
import com.hao_xiao_zi.intellistorecopichelper.model.enums.SpaceTypeEnum;
import com.hao_xiao_zi.intellistorecopichelper.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author 34255
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-07-14 14:32:59
*/
public interface SpaceService extends IService<Space> {

    void validSpace(Space space,Boolean add);

    void fillSpaceBySpaceLevel(Space space);

    QueryWrapper<Space> getQueryWrapper(SpaceQueryDTO queryDTO);

    void spaceDelete(Long id, HttpServletRequest request);

    void spaceUpdate(SpaceUpdateDTO spaceUpdateDTO, HttpServletRequest request);

    void spaceEdit(SpaceEditDTO spaceEditDTO, HttpServletRequest request);

    Long spaceCreate(SpaceCreateDTO spaceCreateDTO, User loginUser);

    Space getSpaceById(Long id);

    IPage<SpaceVO> spacePageQuery(SpaceQueryDTO spaceQueryDTO);

    // 根据用户ID查询空间列表
    SpaceVO spacePageQueryByUserId(SpaceQueryByUserDTO spaceQueryByUserDTO, User loginUser);
}
