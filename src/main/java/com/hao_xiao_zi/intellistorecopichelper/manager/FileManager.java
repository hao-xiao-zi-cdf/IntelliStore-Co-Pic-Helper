package com.hao_xiao_zi.intellistorecopichelper.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.*;
import com.hao_xiao_zi.intellistorecopichelper.config.CosClientConfig;
import com.hao_xiao_zi.intellistorecopichelper.exception.BusinessException;
import com.hao_xiao_zi.intellistorecopichelper.exception.ErrorCode;
import com.hao_xiao_zi.intellistorecopichelper.exception.ThrowUtils;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-07
 * Time: 14:13
 */
@Service
@Slf4j
public class FileManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    /**
     * 上传图片文件并存储到指定路径前缀的存储位置
     *
     * @param multipartFile    上传的图片文件对象
     * @param uploadPathPrefix 文件存储路径前缀
     * @return UploadPictureResult 返回包含上传结果信息的对象，包含文件存储路径、文件名等元数据
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {

        // 校验上传图片(大小，后缀...)
        VerifyImage(multipartFile);

        // 拼接上传的文件名称(上传日期 + 随机6位字符 + 原始文件名)
        String uploadPictureName = String.format("%s_%s.%s",
                DateUtil.formatDate(new Date()),
                RandomUtil.randomString(6),
                multipartFile.getOriginalFilename());

        // 拼接上传文件路径( /用户id/文件名称 ) 便于区分不同用户上传的图片
        String uploadPicturePath = String.format("/%s/%s", uploadPathPrefix, uploadPictureName);

        // 上传文件，处理返回的图片信息
        File file = null;
        try {
            // 上传文件
            file = File.createTempFile(uploadPicturePath, null);
            multipartFile.transferTo(file);
            PutObjectResult putObjectResult = cosManager.putPicture(uploadPicturePath, file);

            // 解析图片信息，进行对象封装
            return parseImgInfo(putObjectResult, multipartFile, uploadPicturePath);
        } catch (Exception e) {
            log.error("文件上传对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 删除临时文件
            deleteTempFile(file);
        }
    }


//    public UploadPictureResult uploadPictureByURL(String fileURL, String uploadPathPrefix) {
//
//        // 校验上传图片(大小，后缀...)
//        VerifyURL(fileURL);
//
//        // 拼接上传的文件名称(上传日期 + 随机6位字符 + 原始文件名)
//        String uploadPictureName = String.format("%s_%s.%s",
//                DateUtil.formatDate(new Date()),
//                RandomUtil.randomString(6),
//                FileUtil.mainName(fileURL));
//
//        // 拼接上传文件路径( /用户id/文件名称 ) 便于区分不同用户上传的图片
//        String uploadPicturePath = String.format("/%s/%s", uploadPathPrefix, uploadPictureName);
//
//        // 上传文件，处理返回的图片信息
//        File file = null;
//        try {
//            // 上传文件
//            file = File.createTempFile(uploadPicturePath, null);
//            // 根据url下载文件
//            HttpUtil.downloadFile(fileURL,file);
//
//            PutObjectResult putObjectResult = cosManager.putPicture(uploadPicturePath, file);
//
//            // 解析图片信息，进行对象封装
//            return parseImgInfo(putObjectResult, multipartFile, uploadPicturePath);
//        } catch (Exception e) {
//            log.error("文件上传对象存储失败", e);
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
//        } finally {
//            // 删除临时文件
//            deleteTempFile(file);
//        }
//    }

    private void VerifyURL(String fileURL) {

        // 校验参数
        ThrowUtils.throwIf(fileURL == null,new BusinessException(ErrorCode.PARAMS_ERROR));

        // 校验URL合法性
        try {
            new URL(fileURL);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"文件地址格式不正确");
        }

        // 校验请求协议
        ThrowUtils.throwIf(!fileURL.startsWith("http://") && !fileURL.startsWith("https://"),new BusinessException(ErrorCode.PARAMS_ERROR,"仅支持http和https协议"));


        HttpResponse response = null;

        try {
            // 发送head请求获取响应头内容
            response = HttpUtil.createRequest(Method.HEAD, fileURL).execute();

            // 判断是否正常返回
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }

            // 校验文件类型
            String contentType = response.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)) {
                // 允许的图片类型
                final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp");
                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPES.contains(contentType.toLowerCase()),
                        ErrorCode.PARAMS_ERROR, "文件类型错误");
            }

            // 校验文件大小
            String contentLengthStr = response.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    final long TWO_MB = 2 * 1024 * 1024L; // 限制文件大小为 2MB
                    ThrowUtils.throwIf(contentLength > TWO_MB, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式错误");
                }
            }
        } finally {
            // 关闭资源
            if(response != null){
                response.close();
            }
        }
    }

    private void validPicture(String fileUrl) {
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "文件地址不能为空");

        try {
            // 1. 验证 URL 格式
            new URL(fileUrl); // 验证是否是合法的 URL
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址格式不正确");
        }

        // 2. 校验 URL 协议
        ThrowUtils.throwIf(!(fileUrl.startsWith("http://") || fileUrl.startsWith("https://")),
                ErrorCode.PARAMS_ERROR, "仅支持 HTTP 或 HTTPS 协议的文件地址");

        // 3. 发送 HEAD 请求以验证文件是否存在
        HttpResponse response = null;
        try {
            response = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            // 未正常返回，无需执行其他判断
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }

        } finally {
            if (response != null) {
                response.close();
            }
        }
    }


    private UploadPictureResult parseImgInfo(PutObjectResult putObjectResult, MultipartFile multipartFile, String uploadPicturePath) {

        // 获取解析后的图片信息
        ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
        UploadPictureResult uploadPictureResult = new UploadPictureResult();

        int width = imageInfo.getWidth();
        int height = imageInfo.getHeight();
        // 计算宽高比例
        double picScale = NumberUtil.round(width * 1.0 / height, 2).doubleValue();

        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPicturePath);
        uploadPictureResult.setPicName(FileUtil.mainName(multipartFile.getOriginalFilename()));
        uploadPictureResult.setPicSize(multipartFile.getSize());
        uploadPictureResult.setPicWidth(width);
        uploadPictureResult.setPicHeight(height);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());

        return uploadPictureResult;
    }

    private void VerifyImage(MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");

        // 限制上传图片大小
        final long maxSize = 1024 * 1024L;
        long imgSize = multipartFile.getSize();
        ThrowUtils.throwIf(imgSize > 2 * maxSize, new BusinessException(ErrorCode.PARAMS_ERROR, "图片大小的不超过2MB"));

        // 限制上传文件后缀名
        final List<String> suffixList = Arrays.asList("jpeg", "jpg", "png", "webp");
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        ThrowUtils.throwIf(!suffixList.contains(suffix), new BusinessException(ErrorCode.PARAMS_ERROR, "图片后缀不符合"));
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


}

