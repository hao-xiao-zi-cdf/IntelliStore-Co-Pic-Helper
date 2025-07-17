package com.hao_xiao_zi.intellistorecopichelper.model.dto.space.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-17
 * Time: 20:09
 */
@Data
public class SpaceAnalyzeDTO implements Serializable {

    /**
     * 空间 ID
     */
    private Long spaceId;

    /**
     * 是否查询公共图库
     */
    private boolean queryPublic;

    /**
     * 全空间分析
     */
    private boolean queryAll;


    private static final long serialVersionUID = 1L;
}

