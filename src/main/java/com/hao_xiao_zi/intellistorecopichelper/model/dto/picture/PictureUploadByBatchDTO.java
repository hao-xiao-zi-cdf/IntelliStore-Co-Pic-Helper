package com.hao_xiao_zi.intellistorecopichelper.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-11
 * Time: 17:11
 */
@Data
public class PictureUploadByBatchDTO implements Serializable {

    /**
     * 搜索词
     */
    private String searchText;

    /**
     * 抓取数量
     */
    private Integer count = 10;

    /**
     * 名称前缀
     */
    private String namePrefix;


    private static final long serialVersionUID = 2874125710013230829L;
}
