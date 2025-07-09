package com.hao_xiao_zi.intellistorecopichelper.model.enums;

import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-05
 * Time: 14:06
 */
@Getter
public enum UserRoleEnum {

    USER("user", "普通用户"),
    ADMIN("admin", "管理员");

    /**
     * 含义
     */
    private final String mean;

    /**
     * 角色
     */
    private final String value;

    UserRoleEnum(String value, String mean) {
        this.value = value;
        this.mean = mean;
    }

    /**
     * 根据value值查询用户身份
     *
     * @param value 角色
     * @return 用户身份
     */
    public static UserRoleEnum getUserRoleByEnum(String value) {
        if (ObjectUtil.isEmpty(value)) {
            return null;
        }
        for (UserRoleEnum userRole : UserRoleEnum.values()) {
            if (ObjectUtil.equal(value, userRole.value)) {
                return userRole;
            }
        }
        return null;
    }
}
