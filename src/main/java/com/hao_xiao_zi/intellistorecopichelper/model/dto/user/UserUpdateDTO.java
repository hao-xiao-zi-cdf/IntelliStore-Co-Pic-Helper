package com.hao_xiao_zi.intellistorecopichelper.model.dto.user;
import lombok.Data;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-05
 * Time: 22:33
 */
@Data
public class UserUpdateDTO implements Serializable {

    /**
     * id
     */
    private Long id;

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
     * 用户角色：user/admin/vip
     */
    private String userRole;

    private static final long serialVersionUID = 4466855079439467638L;
}
