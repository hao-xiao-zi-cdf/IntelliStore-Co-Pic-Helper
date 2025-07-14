package com.hao_xiao_zi.intellistorecopichelper.model.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-14
 * Time: 14:38
 */
@Data
public class SpaceEditDTO implements Serializable {

    /**
     * 空间id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;


    private static final long serialVersionUID = 6242655628630634742L;

}
