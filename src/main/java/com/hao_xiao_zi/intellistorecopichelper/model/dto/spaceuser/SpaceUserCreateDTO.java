package com.hao_xiao_zi.intellistorecopichelper.model.dto.spaceuser;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-19
 * Time: 14:23
 */
@Data
@Builder
public class SpaceUserCreateDTO implements Serializable {

    /**
     * 空间 ID
     */
    private Long spaceId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;


    private static final long serialVersionUID = 1L;
}

