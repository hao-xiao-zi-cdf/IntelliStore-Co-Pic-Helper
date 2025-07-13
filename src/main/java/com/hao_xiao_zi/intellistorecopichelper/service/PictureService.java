package com.hao_xiao_zi.intellistorecopichelper.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.picture.*;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.User;
import com.hao_xiao_zi.intellistorecopichelper.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 34255
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-07-07 12:51:56
 */
public interface PictureService extends IService<Picture> {

    Picture uploadPicture(Object inputSource, PictureUploadDTO pictureUploadDTO, HttpServletRequest request);

    void fillReviewParam(Picture picture, User loginUser);

    void pictureDelete(Long id, HttpServletRequest request);

    void clearPictureFile(Picture picture);

    Picture getPictureById(Long id);

    void pictureUpdate(PictrueUpdateDTO pictrueUpdateDTO, HttpServletRequest request);

    UpdateWrapper<Picture> getPictureUpdateWrapper(PictrueUpdateDTO pictrueUpdateDTO);

    IPage<Picture> picturePageQuery(PictureQueryDTO pictureQueryDTO);

    QueryWrapper<Picture> getQueryWrapper(PictureQueryDTO pictureQueryDTO);

    IPage<PictureVO> picturePageVoQuery(PictureQueryDTO pictureQueryDTO);

    void pictureEdit(PictrueUpdateDTO pictrueUpdateDTO, HttpServletRequest request);

    void PictureReview(PictureReviewDTO pictureReviewDTO, HttpServletRequest request);

    PictureVO getPictureVOById(Long id);

    Integer PictureUploadByBatch(PictureUploadByBatchDTO pictureUploadByBatchDTO, HttpServletRequest request);

    IPage<PictureVO> picturePageVoQueryByCache(PictureQueryDTO pictureQueryDTO);
}
