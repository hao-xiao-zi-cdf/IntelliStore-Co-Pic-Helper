package com.hao_xiao_zi.intellistorecopichelper.model.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-23
 * Time: 15:53
 */
@Data
public class SpaceQueryByUserDTO implements Serializable {

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 空间类型
     */
    private Integer spaceType;
}
