package com.hao_xiao_zi.intellistorecopichelper.model.dto.file;

import lombok.Builder;
import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-07
 * Time: 14:14
 */
@Data
public class UploadPictureResult {

    /**
     * 图片 url地址
     */
    private String url;

    /**
     * 缩略图 url地址
     */
    private String thumbnailUrl;

    /**
     * 图片名称
     */
    private String picName;

    /**
     * 文件体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private int picWidth;

    /**
     * 图片高度
     */
    private int picHeight;

    /**
     * 图片宽高比
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 图片主色调
     */
    private String picColor;


}

