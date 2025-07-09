package com.hao_xiao_zi.intellistorecopichelper.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-05
 * Time: 14:25
 */
@Data
public class UserRegisterDTO implements Serializable {

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 用户密码
     */
    private String userPassword;

    /**
     * 确认密码
     */
    private String checkPassword;

    private static final long serialVersionUID = -8048001398514167548L;

}
