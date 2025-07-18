package com.hao_xiao_zi.intellistorecopichelper.model.dto.space.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-18
 * Time: 13:36
 */
@Data
public class SpaceRankAnalyzeDTO implements Serializable {

    /**
     * 排名前 N 的空间
     */
    private Integer topN = 10;

    private static final long serialVersionUID = 1L;
}

