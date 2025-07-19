package com.hao_xiao_zi.intellistorecopichelper.controller;

import com.hao_xiao_zi.intellistorecopichelper.common.BaseResponse;
import com.hao_xiao_zi.intellistorecopichelper.common.DeleteRequest;
import com.hao_xiao_zi.intellistorecopichelper.common.ResultUtils;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.spaceuser.SpaceUserCreateDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.spaceuser.SpaceUserEditDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.spaceuser.SpaceUserQueryDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.SpaceUser;
import com.hao_xiao_zi.intellistorecopichelper.model.vo.SpaceUserVO;
import com.hao_xiao_zi.intellistorecopichelper.service.SpaceUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-19
 * Time: 15:27
 */
@RestController
@RequestMapping("/spaceUser")
@Slf4j
public class SpaceUserController {

    @Resource
    private SpaceUserService spaceUserService;

    /**
     * 添加成员到空间
     */
    @PostMapping("/add")
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserCreateDTO spaceUserCreateDTO, HttpServletRequest request) {
        long id = spaceUserService.CreateSpaceUser(spaceUserCreateDTO);
        return ResultUtils.success(id);
    }

    /**
     * 从空间移除成员
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpaceUser(@RequestBody DeleteRequest deleteRequest,
                                                 HttpServletRequest request) {
        spaceUserService.deleteSpaceUser(deleteRequest);
        return ResultUtils.success(true);
    }

    /**
     * 查询某个成员在某个空间的信息
     */
    @PostMapping("/get")
    public BaseResponse<SpaceUser> getSpaceUser(@RequestBody SpaceUserQueryDTO spaceUserQueryDTO) {
        SpaceUser spaceUser = spaceUserService.getUser(spaceUserQueryDTO);
        return ResultUtils.success(spaceUser);
    }

    /**
     * 查询成员信息列表
     */
    @PostMapping("/list")
    public BaseResponse<List<SpaceUserVO>> listSpaceUser(@RequestBody SpaceUserQueryDTO spaceUserQueryDTO) {
        List<SpaceUser> spaceUserList = spaceUserService.getSpaceUsers(spaceUserQueryDTO);
        return ResultUtils.success(spaceUserService.getSpaceUserVOList(spaceUserList));
    }

    /**
     * 编辑成员信息（设置权限）
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditDTO spaceUserEditDTO) {
        spaceUserService.SpaceUserEdit(spaceUserEditDTO);
        return ResultUtils.success(true);
    }

    /**
     * 查询我加入的团队空间列表
     */
    @PostMapping("/list/my")
    public BaseResponse<List<SpaceUserVO>> listMyTeamSpace(HttpServletRequest request) {
        List<SpaceUser> spaceUserList = spaceUserService.listMyTeamSpace(request);
        return ResultUtils.success(spaceUserService.getSpaceUserVOList(spaceUserList));
    }

}

