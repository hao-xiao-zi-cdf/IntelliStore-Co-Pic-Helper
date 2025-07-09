package com.hao_xiao_zi.intellistorecopichelper.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-07
 * Time: 14:01
 */
@Data
public class PictureUploadDTO implements Serializable {

    /**
     * ID 未上传id-创建图片 上传id-修改已上传文件对象
     */
    private Long id;

    private static final long serialVersionUID = 9122665114071857044L;
}
