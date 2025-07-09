package com.hao_xiao_zi.intellistorecopichelper.model.dto.user;
import lombok.Data;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-05
 * Time: 22:28
 */
@Data
public class UserCreateDTO implements Serializable {

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 分享码
     */
    private String shareCode;

    /**
     * 用户角色：user/admin/vip
     */
    private String userRole;

    private static final long serialVersionUID = 1675356409818891234L;
}
