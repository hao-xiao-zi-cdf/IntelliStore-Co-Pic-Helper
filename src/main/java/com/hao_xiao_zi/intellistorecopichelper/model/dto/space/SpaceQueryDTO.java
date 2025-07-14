package com.hao_xiao_zi.intellistorecopichelper.model.dto.space;

import com.hao_xiao_zi.intellistorecopichelper.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-14
 * Time: 14:38
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SpaceQueryDTO extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;


    private static final long serialVersionUID = -6511193580525389899L;
}