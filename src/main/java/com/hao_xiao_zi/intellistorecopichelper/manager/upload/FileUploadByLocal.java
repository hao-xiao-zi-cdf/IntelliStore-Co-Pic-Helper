package com.hao_xiao_zi.intellistorecopichelper.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.hao_xiao_zi.intellistorecopichelper.exception.BusinessException;
import com.hao_xiao_zi.intellistorecopichelper.exception.ErrorCode;
import com.hao_xiao_zi.intellistorecopichelper.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-11
 * Time: 15:06
 */
@Service
@Slf4j
public class FileUploadByLocal extends FileUploadTemplate{

    @Override
    protected void VerifyImage(Object inputSource) {
        ThrowUtils.throwIf(inputSource == null, ErrorCode.PARAMS_ERROR, "文件不能为空");

        // 限制上传图片大小
        MultipartFile multipartFile = (MultipartFile)inputSource;
        final long maxSize = 1024 * 1024L;
        long imgSize = multipartFile.getSize();
        ThrowUtils.throwIf(imgSize > 2 * maxSize, new BusinessException(ErrorCode.PARAMS_ERROR, "图片大小的不超过2MB"));

        // 限制上传文件后缀名
        final List<String> suffixList = Arrays.asList("jpeg", "jpg", "png", "webp");
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        ThrowUtils.throwIf(!suffixList.contains(suffix), new BusinessException(ErrorCode.PARAMS_ERROR, "图片后缀不符合"));
    }

    @Override
    protected String getFileName(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }

    @Override
    protected void storageToLocal(Object inputSource, File file){
        MultipartFile multipartFile = (MultipartFile) inputSource;
        try {
            multipartFile.transferTo(file);
        } catch (IOException e) {
            log.error("文件上传对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        }
    }
}
