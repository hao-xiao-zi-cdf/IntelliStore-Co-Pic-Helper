package com.hao_xiao_zi.intellistorecopichelper.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-06
 * Time: 11:08
 */
@Data
@AllArgsConstructor
public class PageResult {

    private long total; //总记录数

    private List records; //当前页数据集合
}
