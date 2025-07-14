package com.hao_xiao_zi.intellistorecopichelper.model.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-14
 * Time: 14:37
 */
@Data
public class SpaceCreateDTO implements Serializable {

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;


    private static final long serialVersionUID = -650687103343228508L;
}
