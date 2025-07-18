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
 * Time: 13:30
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceUserAnalyzeVO implements Serializable {

    /**
     * 时间区间
     */
    private String period;

    /**
     * 上传数量
     */
    private Long count;


    private static final long serialVersionUID = 1L;
}

