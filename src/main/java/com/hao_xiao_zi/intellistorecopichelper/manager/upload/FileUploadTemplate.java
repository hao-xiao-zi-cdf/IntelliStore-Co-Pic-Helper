package com.hao_xiao_zi.intellistorecopichelper.manager.upload;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-11
 * Time: 14:34
 */

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.hao_xiao_zi.intellistorecopichelper.config.CosClientConfig;
import com.hao_xiao_zi.intellistorecopichelper.exception.BusinessException;
import com.hao_xiao_zi.intellistorecopichelper.exception.ErrorCode;
import com.hao_xiao_zi.intellistorecopichelper.manager.CosManager;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;


/**
 * 文件上传通用模板
 */
@Slf4j
public abstract class FileUploadTemplate {

    @Resource
    public CosManager cosManager;

    @Resource
    public CosClientConfig cosClientConfig;

    public UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {

        // 校验上传图片(大小，后缀...)
        VerifyImage(inputSource);

        // 拼接上传的文件名称(上传日期 + 随机6位字符 + 原始文件后缀名)
        String uploadPictureName = String.format("%s_%s.%s",
                DateUtil.formatDate(new Date()),
                RandomUtil.randomString(6),
                getFileSuffix(inputSource));

        // 拼接上传文件路径( /用户id/文件名称 ) 便于区分不同用户上传的图片
        String uploadPicturePath = String.format("/%s/%s", uploadPathPrefix, uploadPictureName);

        // 上传文件，处理返回的图片信息
        File file = null;
        try {
            // 上传文件
            file = File.createTempFile(uploadPicturePath, null);

            // 存储到本地临时文件
            storageToLocal(inputSource,file);

            // 上传文件到对象存储
            PutObjectResult putObjectResult = cosManager.putPicture(uploadPicturePath, file);

            // 获取对象存储解析后的图片信息
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            UploadPictureResult uploadPictureResult = parseImgProcessInfo(processResults, getFileName(inputSource), imageInfo);
            if(uploadPictureResult != null){
                // 压缩成功，返回压缩后的信息
                return uploadPictureResult;
            }

            // 压缩失败，返回原图信息
            return parseImgInfo(imageInfo,file,uploadPicturePath);
        } catch (Exception e) {
            log.error("文件上传对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 删除临时文件
            deleteTempFile(file);
        }
    }

    // 解析图片处理后信息
    private UploadPictureResult parseImgProcessInfo(ProcessResults processResults, String fileName, ImageInfo imageInfo) {

        UploadPictureResult uploadPictureResult = new UploadPictureResult();

        // 获取所有处理规则的处理结果，一条处理规则对应一条处理结果
        List<CIObject> CIObjectList = processResults.getObjectList();
        if(CollUtil.isEmpty(CIObjectList)){
            return null;
        }

        // 压缩处理结果
        CIObject compressResult = CIObjectList.get(0);
        CIObject thumbnailResult = CIObjectList.get(0);
        if(CIObjectList.size() > 1){
            // 缩略处理结果
            thumbnailResult = CIObjectList.get(1);
        }

        // 计算宽高比例
        int width = compressResult.getWidth();
        int height = compressResult.getHeight();
        double picScale = NumberUtil.round(width * 1.0 / height, 2).doubleValue();

        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressResult.getKey());
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailResult.getKey());
        uploadPictureResult.setPicName(fileName);
        uploadPictureResult.setPicSize(compressResult.getSize().longValue());
        uploadPictureResult.setPicWidth(width);
        uploadPictureResult.setPicHeight(height);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(compressResult.getFormat());
        // 获取图片颜色
        uploadPictureResult.setPicColor(imageInfo.getAve());

        return uploadPictureResult;
    }

    // 解析图片信息
    private UploadPictureResult parseImgInfo(ImageInfo imageInfo,File file,String uploadPicturePath) {

        UploadPictureResult uploadPictureResult = new UploadPictureResult();

        int width = imageInfo.getWidth();
        int height = imageInfo.getHeight();
        // 计算宽高比例
        double picScale = NumberUtil.round(width * 1.0 / height, 2).doubleValue();

        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPicturePath);
        uploadPictureResult.setPicName(FileUtil.mainName(file));
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setPicWidth(width);
        uploadPictureResult.setPicHeight(height);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());

        return uploadPictureResult;
    }

    // 删除临时文件
    private void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        // 删除临时文件
        boolean delete = file.delete();
        if (!delete) {
            log.error("file delete error, filepath = {}", file.getAbsoluteFile());
        }
    }

    // 获取文件名称
    private String getFileName(Object inputSource) {
        if (inputSource instanceof String) {
            // url
            return FileUtil.mainName((String)inputSource);
        }
        MultipartFile file = (MultipartFile)inputSource;
        return FileUtil.mainName(file.getOriginalFilename());
    }

    /**
     * 检验上传图片（后缀名，大小）
     * @param inputSource 输入源
     */
    protected abstract void VerifyImage(Object inputSource);

    /**
     * 获取文件名称
     * @param inputSource 输入源
     */
    protected abstract String getFileSuffix(Object inputSource);

    /**
     * 存储到本地临时文件
     * @param inputSource 输入源
     * @param file 本地临时文件
     */
    protected abstract void storageToLocal(Object inputSource, File file);
}
