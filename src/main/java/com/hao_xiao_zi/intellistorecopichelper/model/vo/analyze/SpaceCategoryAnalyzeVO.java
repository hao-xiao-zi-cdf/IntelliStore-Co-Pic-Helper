package com.hao_xiao_zi.intellistorecopichelper.model.vo.analyze;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-17
 * Time: 21:57
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SpaceCategoryAnalyzeVO implements Serializable {

    /**
     * 图片分类
     */
    private String category;

    /**
     * 图片数量
     */
    private Long count;

    /**
     * 分类图片总大小
     */
    private Long totalSize;


    private static final long serialVersionUID = 1L;
}
