package com.hao_xiao_zi.intellistorecopichelper.controller;

import com.hao_xiao_zi.intellistorecopichelper.common.BaseResponse;
import com.hao_xiao_zi.intellistorecopichelper.common.ResultUtils;
import com.hao_xiao_zi.intellistorecopichelper.exception.BusinessException;
import com.hao_xiao_zi.intellistorecopichelper.exception.ErrorCode;
import com.hao_xiao_zi.intellistorecopichelper.exception.ThrowUtils;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.space.*;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.Space;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.User;
import com.hao_xiao_zi.intellistorecopichelper.model.vo.SpaceVO;
import com.hao_xiao_zi.intellistorecopichelper.service.SpaceService;
import com.hao_xiao_zi.intellistorecopichelper.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

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
    @PostMapping
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
}
