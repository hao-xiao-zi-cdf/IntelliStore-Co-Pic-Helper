package com.hao_xiao_zi.intellistorecopichelper.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.user.UserCreateDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.user.UserEditDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.user.UserQueryDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.user.UserUpdateDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hao_xiao_zi.intellistorecopichelper.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author 34255
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2025-07-05 13:43:32
 */
public interface UserService extends IService<User> {

    Boolean userRegister(String userAccount, String userPassword, String checkPassword);

    String getEncryptPassword(String userPassword);

    UserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    User getLoginUser(HttpServletRequest request);

    boolean userLogout(HttpServletRequest request);

    void userCreate(UserCreateDTO userCreateDTO);

    void userRemove(Long id);

    void userUpdate(UserUpdateDTO userUpdateDTO);

    UpdateWrapper<User> getUseUpdateWrapper(UserUpdateDTO userUpdateDTO);

    User getUserById(Long id);

    List<UserVO> getUserVOList(List<User> userList);

    QueryWrapper<User> getQueryWrapper(UserQueryDTO userQueryDTO);

    IPage<UserVO> userPageQuery(UserQueryDTO userQueryDTO);

    Boolean isAdmin(User user);

    void userEdit(UserEditDTO userEditDTO, HttpServletRequest request);
}
