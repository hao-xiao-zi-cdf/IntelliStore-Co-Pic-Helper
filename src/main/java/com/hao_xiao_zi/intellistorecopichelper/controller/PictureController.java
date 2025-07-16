package com.hao_xiao_zi.intellistorecopichelper.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpRequest;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hao_xiao_zi.intellistorecopichelper.annotation.AuthCheck;
import com.hao_xiao_zi.intellistorecopichelper.api.imagesearch.ImageSearchApiFacade;
import com.hao_xiao_zi.intellistorecopichelper.api.imagesearch.model.ImageSearchResult;
import com.hao_xiao_zi.intellistorecopichelper.common.BaseResponse;
import com.hao_xiao_zi.intellistorecopichelper.common.PageResult;
import com.hao_xiao_zi.intellistorecopichelper.common.ResultUtils;
import com.hao_xiao_zi.intellistorecopichelper.exception.BusinessException;
import com.hao_xiao_zi.intellistorecopichelper.exception.ErrorCode;
import com.hao_xiao_zi.intellistorecopichelper.exception.ThrowUtils;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.picture.*;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.Picture;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.User;
import com.hao_xiao_zi.intellistorecopichelper.model.vo.PictureVO;
import com.hao_xiao_zi.intellistorecopichelper.model.vo.UserVO;
import com.hao_xiao_zi.intellistorecopichelper.service.PictureService;
import com.hao_xiao_zi.intellistorecopichelper.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Delete;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-07
 * Time: 16:41
 */
@Slf4j
@Api(tags = "图片相关接口")
@RestController
@RequestMapping("/picture")
public class PictureController {

    @Resource
    public PictureService pictureService;

    @Resource
    public UserService userService;

    /**
     * 本地上传图片接口
     *
     * @param multipartFile    要上传的图片文件
     * @param pictureUploadDTO 包含图片ID，用于判断创建或更新图片
     * @param request          获取登录用户
     * @return 返回图片视图对象，包含了图片详细信息
     */
    @PostMapping("/upload/local")
    @ApiOperation("本地图片上传")
    public BaseResponse<PictureVO> uploadPicture(
            @RequestParam MultipartFile multipartFile,
            PictureUploadDTO pictureUploadDTO,
            HttpServletRequest request) {
        Picture picture = pictureService.uploadPicture(multipartFile, pictureUploadDTO, request);
        PictureVO pictureVO = PictureVO.objToVo(picture);
        return ResultUtils.success(pictureVO);
    }

    /**
     * URL上传图片接口
     *
     * @param pictureUploadDTO 包含图片ID，用于判断创建或更新图片
     * @param request          获取登录用户
     * @return 返回图片视图对象，包含了图片详细信息
     */
    @PostMapping("/upload/url")
    @ApiOperation("URL图片上传")
    public BaseResponse<PictureVO> uploadPicture(
            @RequestBody PictureUploadDTO pictureUploadDTO,
            HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadDTO == null || pictureUploadDTO.getFileUrl() == null,new BusinessException(ErrorCode.PARAMS_ERROR));
        String fileURL = pictureUploadDTO.getFileUrl();
        Picture picture = pictureService.uploadPicture(fileURL, pictureUploadDTO, request);
        PictureVO pictureVO = PictureVO.objToVo(picture);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 删除指定ID的图片资源（只限本人和管理员操作）
     *
     * @param id      图片的ID
     * @param request 获取用户信息
     * @return 返回一个BaseResponse对象，包含删除操作的成功与否状态
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除图片（本人和管理员）")
    public BaseResponse<Boolean> pictureDelete(@PathVariable Long id, HttpServletRequest request) {
        pictureService.pictureDelete(id, request);
        return ResultUtils.success(true);
    }

    /**
     * 根据ID查询图片信息(管理员)
     *
     * @param id 图片的唯一标识符
     * @return 包含图片信息的响应对象
     */
    @GetMapping("/{id}")
    @ApiOperation("查询图片信息（管理员）")
    public BaseResponse<Picture> getPictureById(@PathVariable Long id) {
        Picture picture = pictureService.getPictureById(id);
        return ResultUtils.success(picture);
    }

    /**
     * 根据ID查询图片信息(普通用户)
     *
     * @param id 图片的唯一标识符
     * @return 通过审核，包含脱敏后的图片信息的响应对象
     */
    @GetMapping("/vo/{id}")
    @ApiOperation("查询图片信息（普通用户）")
    public BaseResponse<PictureVO> getPictureVoById(@PathVariable Long id, HttpServletRequest request) {
        PictureVO pictureVo = pictureService.getPictureVOById(id, request);
        return ResultUtils.success(pictureVo);
    }

    /**
     * 更新图片信息
     *
     * @param pictrueUpdateDTO 包含更新的图片信息的数据传输对象
     * @return 返回一个BaseResponse对象，其中包含更新操作的成功状态
     */
    @PostMapping
    @ApiOperation("更新图片（管理员）")
    public BaseResponse<Boolean> updatePicture(@RequestBody PictrueUpdateDTO pictrueUpdateDTO,HttpServletRequest request) {
        pictureService.pictureUpdate(pictrueUpdateDTO,request);
        return ResultUtils.success(true);
    }

    /**
     * 编辑图片信息
     *
     * @param pictrueUpdateDTO 包含编辑的图片信息的数据传输对象
     * @return 返回一个BaseResponse对象，其中包含编辑操作的成功状态
     */
    @PostMapping("/edit")
    @ApiOperation("编辑图片（仅限本人）")
    public BaseResponse<Boolean> editPicture(@RequestBody PictrueUpdateDTO pictrueUpdateDTO, HttpServletRequest request) {
        pictureService.pictureEdit(pictrueUpdateDTO, request);
        return ResultUtils.success(true);
    }


    /**
     * 查询图片列表（管理员）
     *
     * @param pictureQueryDTO 图片查询条件封装对象，包含分页信息和查询参数
     * @return 包含图片信息页面的BaseResponse对象
     */
    @PostMapping("/list")
    @ApiOperation("查询图片列表（管理员）")
    public BaseResponse<PageResult> getPictureList(@RequestBody PictureQueryDTO pictureQueryDTO) {
        ThrowUtils.throwIf(pictureQueryDTO == null, new BusinessException(ErrorCode.PARAMS_ERROR));
        // 执行图片信息的分页查询
        Page<Picture> picturePage = (Page<Picture>) pictureService.picturePageQuery(pictureQueryDTO);

        // 检查查询结果是否为空
        if (ObjectUtil.isEmpty(picturePage)) {
            // 如果查询结果为空，返回一个空的PageResult对象
            return ResultUtils.success(new PageResult(0, Collections.emptyList()));
        }

        // 如果查询结果不为空，返回包含总记录数和记录列表的PageResult对象
        return ResultUtils.success(new PageResult(picturePage.getTotal(), picturePage.getRecords()));
    }

    /**
     * 查询图片列表（普通用户）脱敏化
     *
     * @param pictureQueryDTO 图片查询条件封装对象，包含分页信息和查询参数
     * @return 审核通过，包含图片信息页面的BaseResponse对象
     */
//    @PostMapping("/vo/list")
//    @ApiOperation("查询图片列表（普通用户）")
//    public BaseResponse<PageResult> getPictureVoList(@RequestBody PictureQueryDTO pictureQueryDTO) {
//    ThrowUtils.throwIf(pictureQueryDTO == null,new BusinessException(ErrorCode.PARAMS_ERROR));
//        // 执行图片信息的分页查询
//        Page<PictureVO> pictureVoPage = (Page<PictureVO>) pictureService.picturePageVoQuery(pictureQueryDTO);
//
//        // 检查查询结果是否为空
//        if (ObjectUtil.isEmpty(pictureVoPage)) {
//            // 如果查询结果为空，返回一个空的PageResult对象
//            return ResultUtils.success(new PageResult(0, Collections.emptyList()));
//        }
//        return ResultUtils.success(new PageResult(pictureVoPage.getTotal(), pictureVoPage.getRecords()));
//    }

    /**
     * 查询图片列表（普通用户）脱敏化
     *
     * @param pictureQueryDTO 图片查询条件封装对象，包含分页信息和查询参数
     * @return 审核通过，包含图片信息页面的BaseResponse对象
     */
    @PostMapping("/vo/list/")
    @ApiOperation("查询图片列表（普通用户）")
    public BaseResponse<PageResult> getPictureVoListByCache(@RequestBody PictureQueryDTO pictureQueryDTO,HttpServletRequest request) {
        ThrowUtils.throwIf(pictureQueryDTO == null, new BusinessException(ErrorCode.PARAMS_ERROR));
        // 执行图片信息的分页查询
        Page<PictureVO> pictureVoPage = (Page<PictureVO>) pictureService.picturePageVoQueryByCache(pictureQueryDTO,request);

        // 检查查询结果是否为空
        if (ObjectUtil.isEmpty(pictureVoPage)) {
            // 如果查询结果为空，返回一个空的PageResult对象
            return ResultUtils.success(new PageResult(0, Collections.emptyList()));
        }
        return ResultUtils.success(new PageResult(pictureVoPage.getTotal(), pictureVoPage.getRecords()));
    }

    /**
     *
     * @param pictureReviewDTO
     * @param request
     * @return
     */
    @PostMapping("/review")
    @ApiOperation("审核图片（管理员）")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Boolean> PictureReview(@RequestBody PictureReviewDTO pictureReviewDTO,HttpServletRequest request) {
        pictureService.PictureReview(pictureReviewDTO,request);
        return ResultUtils.success(true);
    }

    /**
     * 批量抓取图片
     * @param pictureUploadByBatchDTO
     * @param request
     * @return
     */
    @PostMapping("/fetch")
    @ApiOperation("批量抓取图片（管理员）")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Integer> PictureUploadByBatch(@RequestBody PictureUploadByBatchDTO pictureUploadByBatchDTO, HttpServletRequest request) {
        Integer fetchCount = pictureService.PictureUploadByBatch(pictureUploadByBatchDTO, request);
        return ResultUtils.success(fetchCount);
    }

    /**
     * 以图搜图
     */
    @PostMapping("/search/picture")
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(@RequestBody SearchPictureByPictureDTO searchPictureByPictureDTO) {
        List<ImageSearchResult> resultList = pictureService.searchPictureByPicture(searchPictureByPictureDTO);
        return ResultUtils.success(resultList);
    }

    /**
     * 颜色搜图
     * @param searchPictureByColorDTO 颜色搜图参数
     * @param request 用于获取用户信息
     * @return 颜色搜图结果
     */
    @PostMapping("/search/color")
    public BaseResponse<List<PictureVO>> searchPicTureByColor (@RequestBody SearchPictureByColorDTO searchPictureByColorDTO, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, new BusinessException(ErrorCode.NOT_LOGIN_ERROR));
        List<PictureVO> resultList = pictureService.searchPictureByColor(searchPictureByColorDTO, loginUser);
        return ResultUtils.success(resultList);
    }

    /**
     * 批量编辑图片信息（分类，重命名，标签）
     * @param pictureEditByBatchDTO 图片批量编辑请求参数对象，包含编辑的图片范围以及编辑信息
     * @param request 用于获取当前登录用户信息
     * @return 是否成功
     */
    @PostMapping("/edit/batch")
    @ApiOperation("批量编辑图片信息")
    public BaseResponse<Boolean> pictureEditByBatch (@RequestBody PictureEditByBatchDTO pictureEditByBatchDTO, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        pictureService.pictureEditByBatch(pictureEditByBatchDTO, loginUser);
        return ResultUtils.success(true);
    }
}
