package com.hao_xiao_zi.intellistorecopichelper.model.dto.space.analyze;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-18
 * Time: 13:29
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceUserAnalyzeDTO extends SpaceAnalyzeDTO {

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 时间维度：day / week / month
     */
    private String timeDimension;
}

