package com.hao_xiao_zi.intellistorecopichelper.controller;

import com.hao_xiao_zi.intellistorecopichelper.common.BaseResponse;
import com.hao_xiao_zi.intellistorecopichelper.common.ResultUtils;
import com.hao_xiao_zi.intellistorecopichelper.exception.ErrorCode;
import com.hao_xiao_zi.intellistorecopichelper.exception.ThrowUtils;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.space.analyze.*;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.Space;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.User;
import com.hao_xiao_zi.intellistorecopichelper.model.vo.analyze.*;
import com.hao_xiao_zi.intellistorecopichelper.service.SpaceAnalyzeService;
import com.hao_xiao_zi.intellistorecopichelper.service.UserService;
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
 * Date: 2025-07-17
 * Time: 21:37
 */
@RestController
@RequestMapping("/space/analyze")
public class SpaceAnalyzeController {

    @Resource
    private SpaceAnalyzeService spaceAnalyzeService;

    @Resource
    private UserService userService;

    /**
     * 获取空间使用状态
     */
    @PostMapping("/usage")
    public BaseResponse<SpaceUsageAnalyzeVO> getSpaceUsageAnalyze(
            @RequestBody SpaceUsageAnalyzeDTO spaceUsageAnalyzeDTO,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        SpaceUsageAnalyzeVO spaceUsageAnalyze = spaceAnalyzeService.getSpaceUsageAnalyze(spaceUsageAnalyzeDTO, loginUser);
        return ResultUtils.success(spaceUsageAnalyze);
    }

    @PostMapping("/category")
    public BaseResponse<List<SpaceCategoryAnalyzeVO>> getSpaceCategoryAnalyze(@RequestBody SpaceCategoryAnalyzeDTO spaceCategoryAnalyzeRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        List<SpaceCategoryAnalyzeVO> resultList = spaceAnalyzeService.getSpaceCategoryAnalyze(spaceCategoryAnalyzeRequest, loginUser);
        return ResultUtils.success(resultList);
    }


    @PostMapping("/tag")
    public BaseResponse<List<SpaceTagAnalyzeVO>> getSpaceTagAnalyze(@RequestBody SpaceTagAnalyzeDTO spaceTagAnalyzeDTO, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<SpaceTagAnalyzeVO> resultList = spaceAnalyzeService.getSpaceTagAnalyze(spaceTagAnalyzeDTO, loginUser);
        return ResultUtils.success(resultList);
    }

    @PostMapping("/size")
    public BaseResponse<List<SpaceSizeAnalyzeVO>> getSpaceSizeAnalyze(@RequestBody SpaceSizeAnalyzeDTO spaceSizeAnalyzeDTO, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<SpaceSizeAnalyzeVO> resultList = spaceAnalyzeService.getSpaceSizeAnalyze(spaceSizeAnalyzeDTO, loginUser);
        return ResultUtils.success(resultList);
    }

    @PostMapping("/user")
    public BaseResponse<List<SpaceUserAnalyzeVO>> getSpaceUserAnalyze(@RequestBody SpaceUserAnalyzeDTO spaceUserAnalyzeDTO, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<SpaceUserAnalyzeVO> resultList = spaceAnalyzeService.getSpaceUserAnalyze(spaceUserAnalyzeDTO, loginUser);
        return ResultUtils.success(resultList);
    }

    @PostMapping("/rank")
    public BaseResponse<List<Space>> getSpaceRankAnalyze(@RequestBody SpaceRankAnalyzeDTO spaceRankAnalyzeDTO, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<Space> resultList = spaceAnalyzeService.getSpaceRankAnalyze(spaceRankAnalyzeDTO, loginUser);
        return ResultUtils.success(resultList);
    }



}

