package com.hao_xiao_zi.intellistorecopichelper.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-10
 * Time: 14:23
 */
@Getter
public enum PictureReviewStatusEnum {
    
    REVIEWING("待审核", 0),
    PASS("通过", 1),
    REJECT("拒绝", 2);

    /**
     * 含义
     */
    private final String mean;

    /**
     * 审核状态
     */
    private final int value;

    PictureReviewStatusEnum(String mean, int value) {
        this.mean = mean;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举  
     */
    public static PictureReviewStatusEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (PictureReviewStatusEnum pictureReviewStatusEnum : PictureReviewStatusEnum.values()) {
            if (pictureReviewStatusEnum.value == value) {
                return pictureReviewStatusEnum;
            }
        }
        return null;
    }
}
