package com.hao_xiao_zi.intellistorecopichelper.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hao_xiao_zi.intellistorecopichelper.common.DeleteRequest;
import com.hao_xiao_zi.intellistorecopichelper.exception.BusinessException;
import com.hao_xiao_zi.intellistorecopichelper.exception.ErrorCode;
import com.hao_xiao_zi.intellistorecopichelper.exception.ThrowUtils;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.spaceuser.SpaceUserCreateDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.spaceuser.SpaceUserEditDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.spaceuser.SpaceUserQueryDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.Space;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.SpaceUser;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.User;
import com.hao_xiao_zi.intellistorecopichelper.model.enums.SpaceRoleEnum;
import com.hao_xiao_zi.intellistorecopichelper.model.vo.SpaceUserVO;
import com.hao_xiao_zi.intellistorecopichelper.model.vo.SpaceVO;
import com.hao_xiao_zi.intellistorecopichelper.model.vo.UserVO;
import com.hao_xiao_zi.intellistorecopichelper.service.SpaceService;
import com.hao_xiao_zi.intellistorecopichelper.service.SpaceUserService;
import com.hao_xiao_zi.intellistorecopichelper.mapper.SpaceUserMapper;
import com.hao_xiao_zi.intellistorecopichelper.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author 34255
* @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
* @createDate 2025-07-19 14:26:12
*/
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
    implements SpaceUserService{

    @Resource
    private SpaceService spaceService;

    @Resource
    private UserService userService;

    @Override
    public long CreateSpaceUser(SpaceUserCreateDTO spaceUserCreateDTO) {
        // 参数校验
        ThrowUtils.throwIf(spaceUserCreateDTO == null, ErrorCode.PARAMS_ERROR);
        SpaceUser spaceUser = BeanUtil.copyProperties(spaceUserCreateDTO, SpaceUser.class);
        validSpaceUser(spaceUser, true);

        // 数据库操作
        boolean result = this.save(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return spaceUser.getId();
    }

    @Override
    public void deleteSpaceUser(DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        // 判断是否存在
        SpaceUser oldSpaceUser = getById(id);
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public SpaceUser getUser(SpaceUserQueryDTO spaceUserQueryDTO) {
        // 参数校验
        ThrowUtils.throwIf(spaceUserQueryDTO == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceUserQueryDTO.getSpaceId();
        Long userId = spaceUserQueryDTO.getUserId();
        ThrowUtils.throwIf(ObjectUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
        // 查询数据库
        SpaceUser spaceUser = getOne(getQueryWrapper(spaceUserQueryDTO));
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        return spaceUser;
    }

    @Override
    public List<SpaceUser> getSpaceUsers(SpaceUserQueryDTO spaceUserQueryDTO) {
        ThrowUtils.throwIf(spaceUserQueryDTO == null, ErrorCode.PARAMS_ERROR);
        List<SpaceUser> spaceUserList = list(getQueryWrapper(spaceUserQueryDTO));
        return spaceUserList;
    }

    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean add) {
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR);
        // 创建时，空间 id 和用户 id 必填
        Long spaceId = spaceUser.getSpaceId();
        Long userId = spaceUser.getUserId();
        if (add) {
            ThrowUtils.throwIf(ObjectUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
            User user = userService.getById(userId);
            ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 校验是否已添加该成员
            boolean exists = query().eq("spaceId", spaceId)
                    .eq("userId", userId).exists();
            ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "已添加该成员");
        }
        // 校验空间角色
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum spaceRoleEnum = SpaceRoleEnum.getEnumByValue(spaceRole);
        if (spaceRole != null && spaceRoleEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间角色不存在");
        }
    }

    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryDTO spaceUserQueryDTO) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if (spaceUserQueryDTO == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceUserQueryDTO.getId();
        Long spaceId = spaceUserQueryDTO.getSpaceId();
        Long userId = spaceUserQueryDTO.getUserId();
        String spaceRole = spaceUserQueryDTO.getSpaceRole();
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceRole), "spaceRole", spaceRole);
        return queryWrapper;
    }

    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request) {
        // 对象转封装类
        SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);
        // 关联查询用户信息
        Long userId = spaceUser.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
            spaceUserVO.setUser(userVO);
        }
        // 关联查询空间信息
        Long spaceId = spaceUser.getSpaceId();
        if (spaceId != null && spaceId > 0) {
            Space space = spaceService.getById(spaceId);
            SpaceVO spaceVO = BeanUtil.copyProperties(space, SpaceVO.class);
            UserVO userVO = BeanUtil.copyProperties(userService.getUserById(spaceVO.getUserId()), UserVO.class);
            spaceVO.setUser(userVO);
            spaceUserVO.setSpace(spaceVO);
        }
        return spaceUserVO;
    }

    @Override
    public void SpaceUserEdit(SpaceUserEditDTO spaceUserEditDTO) {
        if (spaceUserEditDTO == null || spaceUserEditDTO.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        SpaceUser spaceUser = BeanUtil.copyProperties(spaceUserEditDTO, SpaceUser.class);
        // 数据校验
        validSpaceUser(spaceUser, false);
        // 判断是否存在
        long id = spaceUserEditDTO.getId();
        SpaceUser oldSpaceUser = getById(id);
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = updateById(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public List<SpaceUser> listMyTeamSpace(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        SpaceUserQueryDTO spaceUserQueryDTO = new SpaceUserQueryDTO();
        spaceUserQueryDTO.setUserId(loginUser.getId());

        return list(getQueryWrapper(spaceUserQueryDTO));
    }

    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {
        // 判断输入列表是否为空
        if (CollUtil.isEmpty(spaceUserList)) {
            return Collections.emptyList();
        }
        // 对象列表 => 封装对象列表
        List<SpaceUserVO> spaceUserVOList = spaceUserList.stream().map(SpaceUserVO::objToVo).collect(Collectors.toList());
        // 1. 收集需要关联查询的用户 ID 和空间 ID
        Set<Long> userIdSet = spaceUserList.stream().map(SpaceUser::getUserId).collect(Collectors.toSet());
        Set<Long> spaceIdSet = spaceUserList.stream().map(SpaceUser::getSpaceId).collect(Collectors.toSet());
        // 2. 批量查询用户和空间
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        Map<Long, List<Space>> spaceIdSpaceListMap = spaceService.listByIds(spaceIdSet).stream()
                .collect(Collectors.groupingBy(Space::getId));
        // 3. 填充 SpaceUserVO 的用户和空间信息
        spaceUserVOList.forEach(spaceUserVO -> {
            Long userId = spaceUserVO.getUserId();
            Long spaceId = spaceUserVO.getSpaceId();
            // 填充用户信息
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceUserVO.setUser(BeanUtil.copyProperties(user, UserVO.class));
            // 填充空间信息
            Space space = null;
            if (spaceIdSpaceListMap.containsKey(spaceId)) {
                space = spaceIdSpaceListMap.get(spaceId).get(0);
            }
            spaceUserVO.setSpace(SpaceVO.objToVo(space));
        });
        return spaceUserVOList;
    }


}




