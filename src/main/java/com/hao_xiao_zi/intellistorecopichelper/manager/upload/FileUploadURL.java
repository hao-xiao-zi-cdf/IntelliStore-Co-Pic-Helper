package com.hao_xiao_zi.intellistorecopichelper.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.hao_xiao_zi.intellistorecopichelper.exception.BusinessException;
import com.hao_xiao_zi.intellistorecopichelper.exception.ErrorCode;
import com.hao_xiao_zi.intellistorecopichelper.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-11
 * Time: 15:05
 */
@Service
public class FileUploadURL extends FileUploadTemplate{

    private String picType;

    @Override
    protected void VerifyImage(Object inputSource) {
        // 校验参数
        ThrowUtils.throwIf(inputSource == null,new BusinessException(ErrorCode.PARAMS_ERROR));
        String fileURL = (String)inputSource;

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
                String target = findMatchInList(ALLOW_CONTENT_TYPES, contentType);
                ThrowUtils.throwIf(target == null, ErrorCode.PARAMS_ERROR, "文件类型错误");
                // 截取图片格式
                picType = "." + target.substring(6);
            }

            // 校验文件大小
            String contentLengthStr = response.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    final long TWO_MB = 10 * 1024 * 1024L; // 限制文件大小为 10MB
                    ThrowUtils.throwIf(contentLength > TWO_MB, ErrorCode.PARAMS_ERROR, "文件大小不能超过 10M");
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

    private static String findMatchInList(List<String> list, String target) {
        for (String item : list) {
            if (item.equalsIgnoreCase(target)) {
                return item;
            }
        }
        return null;
    }


    @Override
    protected String getFileSuffix(Object inputSource) {
        return picType;
    }

    @Override
    protected void storageToLocal(Object inputSource, File file) {
        HttpUtil.downloadFile((String)inputSource,file);
    }
}
