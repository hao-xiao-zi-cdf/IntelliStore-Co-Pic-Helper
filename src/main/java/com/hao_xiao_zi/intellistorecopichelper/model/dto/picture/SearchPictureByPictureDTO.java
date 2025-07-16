package com.hao_xiao_zi.intellistorecopichelper.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-16
 * Time: 16:42
 */
@Data
public class SearchPictureByPictureDTO implements Serializable {

    /**
     * 图片 id
     */
    private Long pictureId;

    private static final long serialVersionUID = 1L;
}

