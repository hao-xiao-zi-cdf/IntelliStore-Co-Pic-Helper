package com.hao_xiao_zi.intellistorecopichelper.model.dto.picture;

import com.hao_xiao_zi.intellistorecopichelper.model.entity.Space;
import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-16
 * Time: 19:40
 */
@Data
public class SearchPictureByColorDTO {

    /**
     * 空间
     */
    private Long spaceId;

    /**
     * 颜色
     */
    private String color;
}
