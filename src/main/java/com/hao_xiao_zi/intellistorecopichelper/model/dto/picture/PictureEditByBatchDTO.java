package com.hao_xiao_zi.intellistorecopichelper.model.dto.picture;

import com.hao_xiao_zi.intellistorecopichelper.model.entity.Space;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-16
 * Time: 21:59
 */
@Data
public class PictureEditByBatchDTO implements Serializable {

    /**
     * 图片id列表
     */
    private List<Long> idList;

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签
     */
    private List<String> tag;

    /**
     * 命名规则
     */
    private String nameRule;


    private static final long serialVersionUID = -7422721210552020436L;
}
