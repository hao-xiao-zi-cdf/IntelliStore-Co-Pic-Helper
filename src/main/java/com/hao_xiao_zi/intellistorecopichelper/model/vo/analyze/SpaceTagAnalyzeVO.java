package com.hao_xiao_zi.intellistorecopichelper.model.vo.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-18
 * Time: 10:53
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceTagAnalyzeVO implements Serializable {

    /**
     * 标签名称
     */
    private String tag;

    /**
     * 使用次数
     */
    private Long count;


    private static final long serialVersionUID = 1L;
}

