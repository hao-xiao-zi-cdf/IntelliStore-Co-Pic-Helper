package com.hao_xiao_zi.intellistorecopichelper.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hao_xiao_zi.intellistorecopichelper.common.DeleteRequest;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.spaceuser.SpaceUserCreateDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.spaceuser.SpaceUserEditDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.spaceuser.SpaceUserQueryDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hao_xiao_zi.intellistorecopichelper.model.vo.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 34255
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-07-19 14:26:12
*/
public interface SpaceUserService extends IService<SpaceUser> {

    long CreateSpaceUser(SpaceUserCreateDTO spaceUserCreateDTO);

    void deleteSpaceUser(DeleteRequest deleteRequest);

    SpaceUser getUser(SpaceUserQueryDTO spaceUserQueryDTO);

    List<SpaceUser> getSpaceUsers(SpaceUserQueryDTO spaceUserQueryDTO);

    void validSpaceUser(SpaceUser spaceUser, boolean add);

    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryDTO spaceUserQueryDTO);

    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    void SpaceUserEdit(SpaceUserEditDTO spaceUserEditDTO);

    List<SpaceUser> listMyTeamSpace(HttpServletRequest request);

    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);
}
