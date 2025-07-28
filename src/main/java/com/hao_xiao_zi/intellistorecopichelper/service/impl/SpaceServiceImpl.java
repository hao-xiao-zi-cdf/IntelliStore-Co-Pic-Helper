package com.hao_xiao_zi.intellistorecopichelper.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hao_xiao_zi.intellistorecopichelper.exception.BusinessException;
import com.hao_xiao_zi.intellistorecopichelper.exception.ErrorCode;
import com.hao_xiao_zi.intellistorecopichelper.exception.ThrowUtils;
import com.hao_xiao_zi.intellistorecopichelper.manager.sharding.DynamicShardingManager;
import com.hao_xiao_zi.intellistorecopichelper.mapper.SpaceUserMapper;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.space.*;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.spaceuser.SpaceUserCreateDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.Picture;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.Space;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.SpaceUser;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.User;
import com.hao_xiao_zi.intellistorecopichelper.model.enums.SpaceLevelEnum;
import com.hao_xiao_zi.intellistorecopichelper.model.enums.SpaceRoleEnum;
import com.hao_xiao_zi.intellistorecopichelper.model.enums.SpaceTypeEnum;
import com.hao_xiao_zi.intellistorecopichelper.model.vo.SpaceVO;
import com.hao_xiao_zi.intellistorecopichelper.model.vo.UserVO;
import com.hao_xiao_zi.intellistorecopichelper.service.SpaceService;
import com.hao_xiao_zi.intellistorecopichelper.mapper.SpaceMapper;
import com.hao_xiao_zi.intellistorecopichelper.service.SpaceUserService;
import com.hao_xiao_zi.intellistorecopichelper.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
* @author 34255
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2025-07-14 14:32:59
*/
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceService{

    @Resource
    public UserService userService;
    
    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    @Lazy
    private SpaceUserService spaceUserService;

    @Resource
    @Lazy
    private DynamicShardingManager dynamicShardingManager;
    
    @Override
    public Long spaceCreate(SpaceCreateDTO spaceCreateDTO, User loginUser) {

        // 参数校验
        ThrowUtils.throwIf(spaceCreateDTO == null,new BusinessException(ErrorCode.PARAMS_ERROR));

        // 默认值
        if (StrUtil.isBlank(spaceCreateDTO.getSpaceName())) {
            spaceCreateDTO.setSpaceName("默认空间");
        }
        if (spaceCreateDTO.getSpaceLevel() == null) {
            spaceCreateDTO.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        if(spaceCreateDTO.getSpaceType() == null){
            spaceCreateDTO.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }

        // 权限校验
        if(!spaceCreateDTO.getSpaceLevel().equals(SpaceLevelEnum.COMMON.getValue()) && !userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"非管理员，无权限创建非普通版的私人空间");
        }

        // 校验创建数据
        Space space = BeanUtil.copyProperties(spaceCreateDTO, Space.class);
        space.setUserId(loginUser.getId());
        validSpace(space,true);

        // 根据级别填充空间参数
        fillSpaceBySpaceLevel(space);

        // 创建空间，加锁+事务，控制一人一私有和一公共  -- 注意：锁要包住事务，否则可能引发线程安全问题
        // 根据用户id加锁
        String lock = loginUser.getId().toString().intern();
        synchronized (lock){
            Long spaceId = transactionTemplate.execute(status -> {
                // 查询数据库判断用户是否已创建
                boolean isExists = query()
                        .eq("userId", loginUser.getId())
                        .eq("spaceType", spaceCreateDTO.getSpaceType())
                        .exists();
                ThrowUtils.throwIf(isExists, new BusinessException(ErrorCode.OPERATION_ERROR, "每个用户仅能创建一个私有空间和一个共享空间"));
                boolean isOk = save(space);
                ThrowUtils.throwIf(!isOk, ErrorCode.OPERATION_ERROR, "创建空间失败");
                // 判断空间类型是否为团队空间，默认将创建人设置为团队空间管理员
                if(space.getSpaceType() == SpaceTypeEnum.TEAM.getValue()){
                    boolean isSave = spaceUserService.save(SpaceUser.builder()
                            .spaceId(space.getId())
                            .userId(loginUser.getId())
                            .spaceRole(SpaceRoleEnum.ADMIN.getValue()).build());
                    ThrowUtils.throwIf(!isSave, ErrorCode.OPERATION_ERROR, "创建空间失败");
                }
                // 创建分表
                dynamicShardingManager.createSpacePictureTable(space);

                // 返回空间id
                return space.getId();
            });
            return Optional.ofNullable(spaceId).orElse(-1L);
        }
    }

    @Override
    public Space getSpaceById(Long id) {
        // 参数校验
        ThrowUtils.throwIf(id == null || id < 0, new BusinessException(ErrorCode.PARAMS_ERROR));

        // 查询空间是否存在
        Space space = query().eq("id", id).one();
        ThrowUtils.throwIf(space == null, new BusinessException(ErrorCode.PARAMS_ERROR, "空间资源不存在"));

        // 返回空间资源
        return space;
    }

    @Override
    public IPage<SpaceVO> spacePageQuery(SpaceQueryDTO spaceQueryDTO) {

        // 参数校验
        ThrowUtils.throwIf(spaceQueryDTO == null,new BusinessException(ErrorCode.PARAMS_ERROR));

        // 设置分页参数
        Page<Space> spacePage = new Page<>(spaceQueryDTO.getCurrent(), spaceQueryDTO.getPageSize());

        // 设置其他查询条件
        QueryWrapper<Space> queryWrapper = getQueryWrapper(spaceQueryDTO);

        // 查询空间
        Page<Space> page = page(spacePage, queryWrapper);

        // 获取并转化空间VO对象列表
        List<SpaceVO> spaceVOList = page.getRecords().stream().map(space -> SpaceVO.objToVo(space)).collect(Collectors.toList());

        // 填充创建者信息
        spaceVOList.forEach(spaceVO -> spaceVO.setUser(BeanUtil.copyProperties(userService.getById(spaceVO.getUserId()), UserVO.class)));

        // 构造VO分页对象
        Page<SpaceVO> spaceVoPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        spaceVoPage.setRecords(spaceVOList);
        return spaceVoPage;
    }

    @Override
    public SpaceVO spacePageQueryByUserId(SpaceQueryByUserDTO spaceQueryByUserDTO,User loginUser) {

        // 参数校验
        ThrowUtils.throwIf(spaceQueryByUserDTO == null || loginUser == null,new BusinessException(ErrorCode.PARAMS_ERROR));
        ThrowUtils.throwIf(spaceQueryByUserDTO.getUserId() == null || spaceQueryByUserDTO.getUserId() < 0,new BusinessException(ErrorCode.PARAMS_ERROR));
        ThrowUtils.throwIf(spaceQueryByUserDTO.getSpaceType() == null || spaceQueryByUserDTO.getSpaceType() < 0 || spaceQueryByUserDTO.getSpaceType() > 2,new BusinessException(ErrorCode.PARAMS_ERROR));

        // 查询用户，空间是否存在
        User user = userService.getById(spaceQueryByUserDTO.getUserId());
        ThrowUtils.throwIf(user == null,new BusinessException(ErrorCode.PARAMS_ERROR,"用户不存在"));
        // 根据用户id和空间类型查询空间
        Space space = query().eq("userId", spaceQueryByUserDTO.getUserId())
                .eq("spaceType", spaceQueryByUserDTO.getSpaceType())
                .one();
        ThrowUtils.throwIf(space == null,new BusinessException(ErrorCode.PARAMS_ERROR,"空间不存在"));

        // 构造VO对象
        SpaceVO spaceVO = SpaceVO.objToVo(space);

        // 填充用户信息
        spaceVO.setUser(BeanUtil.copyProperties(user, UserVO.class));

        return spaceVO;
    }

    @Override
    public void validSpace(Space space, Boolean add) {
        ThrowUtils.throwIf(space == null,new BusinessException(ErrorCode.PARAMS_ERROR));

        Long id = space.getId();
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        Integer spaceType = space.getSpaceType();
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);

        // 创建空间
        if(add){
            ThrowUtils.throwIf(StrUtil.isBlank(spaceName) || spaceName.length() > 30,new BusinessException(ErrorCode.PARAMS_ERROR,"创建的空间名称不能为空或名称过长"));
            ThrowUtils.throwIf(spaceLevel == null || spaceLevel < 0 || spaceLevel > 2,new BusinessException(ErrorCode.PARAMS_ERROR,"创建的空间级别为空或级别错误"));
            ThrowUtils.throwIf(spaceType != null && spaceTypeEnum == null,new BusinessException(ErrorCode.PARAMS_ERROR,"创建的空间类型类型错误"));
            return;
        }

        ThrowUtils.throwIf(id == null,new BusinessException(ErrorCode.PARAMS_ERROR,"缺少更新的空间id"));
        ThrowUtils.throwIf(StrUtil.isNotBlank(spaceName) && spaceName.length() > 30,new BusinessException(ErrorCode.PARAMS_ERROR,"更新的空间名称过长"));
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        ThrowUtils.throwIf(space == null,new BusinessException(ErrorCode.PARAMS_ERROR));
        
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum level = SpaceLevelEnum.getEnumByValue(spaceLevel);

        if(space.getMaxSize() == null){
            space.setMaxSize(level.getMaxSize());
        }
        
        if(space.getMaxCount() == null){
            space.setMaxCount(level.getMaxCount());
        }
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryDTO spaceQueryDTO) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();

        Long id = spaceQueryDTO.getId();
        Long userId = spaceQueryDTO.getUserId();
        String spaceName = spaceQueryDTO.getSpaceName();
        Integer spaceLevel = spaceQueryDTO.getSpaceLevel();
        String sortField = spaceQueryDTO.getSortField();
        String sortOrder = spaceQueryDTO.getSortOrder();

        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);

        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField),StrUtil.isNotEmpty(sortOrder) && sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public void spaceDelete(Long id, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(ObjectUtil.hasEmpty(id), new BusinessException(ErrorCode.PARAMS_ERROR));

        // 查询空间是否存在
        Space picture = getById(id);
        ThrowUtils.throwIf(picture == null, new BusinessException(ErrorCode.PARAMS_ERROR, "空间资源不存在"));

        // 判断是否为创建人或管理员操作
        User loginUser = userService.getLoginUser(request);
        if (!picture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 删除空间
        boolean isOk = removeById(id);
        ThrowUtils.throwIf(!isOk, new BusinessException(ErrorCode.OPERATION_ERROR, "删除空间失败"));
    }

    @Override
    public void spaceUpdate(SpaceUpdateDTO spaceUpdateDTO, HttpServletRequest request) {

        User loginUser = userService.getLoginUser(request);

        // 参数校验
        ThrowUtils.throwIf(ObjectUtil.hasEmpty(spaceUpdateDTO), ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceUpdateDTO.getId();
        String spaceName = spaceUpdateDTO.getSpaceName();
        ThrowUtils.throwIf(spaceId == null || spaceId < 0, new BusinessException(ErrorCode.PARAMS_ERROR));
        ThrowUtils.throwIf(StrUtil.isNotBlank(spaceName) && spaceName.length() > 30,new BusinessException(ErrorCode.PARAMS_ERROR,"更新的空间名称过长"));

        // 查询空间资源是否存在
        Space space = query().eq("id", spaceId).one();
        ThrowUtils.throwIf(space == null, new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间资源不存在"));

        // 获取更新条件构造器
        UpdateWrapper<Space> wrapper = getSpaceUpdateWrapper(spaceUpdateDTO);

        // 更新
        boolean isOk = update(wrapper);
        ThrowUtils.throwIf(!isOk, new BusinessException(ErrorCode.OPERATION_ERROR, "更新空间失败"));
    }

    private static UpdateWrapper<Space> getSpaceUpdateWrapper(SpaceUpdateDTO spaceUpdateDTO) {
        UpdateWrapper<Space> wrapper = new UpdateWrapper<>();
        Long id = spaceUpdateDTO.getId();
        String spaceName = spaceUpdateDTO.getSpaceName();
        Integer spaceLevel = spaceUpdateDTO.getSpaceLevel();
        Long maxSize = spaceUpdateDTO.getMaxSize();
        Long maxCount = spaceUpdateDTO.getMaxCount();

        wrapper.set(StrUtil.isNotBlank(spaceName),"spaceName",spaceName);
        wrapper.set(maxSize != null,"maxSize",maxSize);
        wrapper.set(spaceLevel != null,"spaceLevel",spaceLevel);
        wrapper.set(maxCount != null,"maxCount",maxCount);
        wrapper.eq(id != null,"id",id);
        return wrapper;
    }

    @Override
    public void spaceEdit(SpaceEditDTO spaceEditDTO, HttpServletRequest request) {

        // 参数校验
        ThrowUtils.throwIf(ObjectUtil.hasEmpty(spaceEditDTO), ErrorCode.NOT_FOUND_ERROR);
        Long id = spaceEditDTO.getId();
        String spaceName = spaceEditDTO.getSpaceName();
        ThrowUtils.throwIf(id == null || id < 0, new BusinessException(ErrorCode.PARAMS_ERROR));
        ThrowUtils.throwIf(StrUtil.isNotBlank(spaceName) && spaceName.length() > 30,new BusinessException(ErrorCode.PARAMS_ERROR,"更新的空间名称过长"));

        // 查询空间资源是否存在
        Space space = query().eq("id", id).one();
        ThrowUtils.throwIf(space == null, new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间资源不存在"));

        // 判断是否为本人操作
        ThrowUtils.throwIf(!userService.getLoginUser(request).getId().equals(space.getUserId()), new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限，非本人操作"));

        // 获取更新条件构造器
        UpdateWrapper<Space> wrapper = new UpdateWrapper<>();
        wrapper.set("spaceName",spaceName);
        wrapper.eq("id",id);

        // 更新
        boolean isOk = update(wrapper);
        ThrowUtils.throwIf(!isOk, new BusinessException(ErrorCode.OPERATION_ERROR, "更新空间失败"));
    }
}




