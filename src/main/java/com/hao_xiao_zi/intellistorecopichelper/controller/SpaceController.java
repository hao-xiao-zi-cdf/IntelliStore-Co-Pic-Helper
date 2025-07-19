package com.hao_xiao_zi.intellistorecopichelper.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hao_xiao_zi.intellistorecopichelper.common.BaseResponse;
import com.hao_xiao_zi.intellistorecopichelper.common.PageResult;
import com.hao_xiao_zi.intellistorecopichelper.common.ResultUtils;
import com.hao_xiao_zi.intellistorecopichelper.exception.BusinessException;
import com.hao_xiao_zi.intellistorecopichelper.exception.ErrorCode;
import com.hao_xiao_zi.intellistorecopichelper.exception.ThrowUtils;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.space.*;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.Space;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.User;
import com.hao_xiao_zi.intellistorecopichelper.model.enums.SpaceLevelEnum;
import com.hao_xiao_zi.intellistorecopichelper.model.vo.SpaceVO;
import com.hao_xiao_zi.intellistorecopichelper.model.vo.UserVO;
import com.hao_xiao_zi.intellistorecopichelper.service.SpaceService;
import com.hao_xiao_zi.intellistorecopichelper.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-07
 * Time: 16:41
 */
@Slf4j
@Api(tags = "空间相关接口")
@RestController
@RequestMapping("/space")
public class SpaceController {

    @Resource
    public SpaceService spaceService;

    @Resource
    public UserService userService;

    @PostMapping
    @ApiOperation("创建空间")
    public BaseResponse<SpaceVO> updateSpace(@RequestBody SpaceCreateDTO spaceCreateDTO, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, new BusinessException(ErrorCode.NOT_LOGIN_ERROR));
        Long spaceId = spaceService.spaceCreate(spaceCreateDTO, loginUser);
        Space space = spaceService.getSpaceById(spaceId);
        return ResultUtils.success(SpaceVO.objToVo(space));
    }

    /**
     * 删除指定ID的空间资源（只限创建人和管理员操作）
     *
     * @param id      空间的ID
     * @param request 获取用户信息
     * @return 返回一个BaseResponse对象，包含删除操作的成功与否状态
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除空间（创建人和管理员）")
    public BaseResponse<Boolean> spaceDelete(@PathVariable Long id, HttpServletRequest request) {
        spaceService.spaceDelete(id, request);
        return ResultUtils.success(true);
    }

    /**
     * 更新空间信息
     *
     * @param spaceUpdateDTO 包含更新的空间信息的数据传输对象
     * @return 返回一个BaseResponse对象，其中包含更新操作的成功状态
     */
    @PostMapping("/update")
    @ApiOperation("更新空间（管理员）")
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateDTO spaceUpdateDTO, HttpServletRequest request) {
        spaceService.spaceUpdate(spaceUpdateDTO, request);
        return ResultUtils.success(true);
    }

    /**
     * 编辑空间信息
     *
     * @param spaceEditDTO 包含编辑的空间信息的数据传输对象
     * @return 返回一个BaseResponse对象，其中包含编辑操作的成功状态
     */
    @PostMapping("/edit")
    @ApiOperation("编辑空间（仅限本人）")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditDTO spaceEditDTO, HttpServletRequest request) {
        spaceService.spaceEdit(spaceEditDTO, request);
        return ResultUtils.success(true);
    }

    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values()) // 获取所有枚举
                .map(spaceLevelEnum -> new SpaceLevel(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getMean(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()))
                .collect(Collectors.toList());
        return ResultUtils.success(spaceLevelList);
    }

    @GetMapping("/list")
    @ApiOperation("查询空间列表（管理员）")
    public BaseResponse<PageResult> listSpace(SpaceQueryDTO spaceQueryDTO) {
        Page<SpaceVO> spaceVoPage = (Page<SpaceVO>)spaceService.spacePageQuery(spaceQueryDTO);
        if(spaceVoPage.getTotal() == 0){
            return ResultUtils.success(new PageResult(0, Collections.emptyList()));
        }
        return ResultUtils.success(new PageResult(spaceVoPage.getTotal(), spaceVoPage.getRecords()));
    }

    @GetMapping("/{id}")
    @ApiOperation("查询空间")
    public BaseResponse<SpaceVO> getSpaceById(@PathVariable Long id) {
        Space space = spaceService.getSpaceById(id);
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 填充用户信息
        spaceVO.setUser(BeanUtil.copyProperties(userService.getUserById(spaceVO.getUserId()), UserVO.class));
        return ResultUtils.success(spaceVO);
    }

    @GetMapping("/my")
    @ApiOperation("查询我的空间（普通用户）")
    public BaseResponse<SpaceVO> getMySpace(SpaceQueryByUserDTO spaceQueryByUserDTO, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(spaceService.spacePageQueryByUserId(spaceQueryByUserDTO, loginUser));
    }
}
