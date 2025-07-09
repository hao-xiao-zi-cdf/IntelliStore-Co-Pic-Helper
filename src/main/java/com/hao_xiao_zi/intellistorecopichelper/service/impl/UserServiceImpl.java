package com.hao_xiao_zi.intellistorecopichelper.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hao_xiao_zi.intellistorecopichelper.exception.BusinessException;
import com.hao_xiao_zi.intellistorecopichelper.exception.ErrorCode;
import com.hao_xiao_zi.intellistorecopichelper.exception.ThrowUtils;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.user.UserCreateDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.user.UserQueryDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.user.UserUpdateDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.User;
import com.hao_xiao_zi.intellistorecopichelper.model.enums.UserRoleEnum;
import com.hao_xiao_zi.intellistorecopichelper.model.vo.UserVO;
import com.hao_xiao_zi.intellistorecopichelper.service.UserService;
import com.hao_xiao_zi.intellistorecopichelper.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author 34255
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-07-05 13:43:32
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 用户注册
     * @param userAccount 用户账号
     * @param userPassword 用户密码
     * @param checkPassword 确认密码，用于验证两次输入的密码是否一致
     */
    @Override
    public Boolean userRegister(String userAccount, String userPassword, String checkPassword) {

        // 1.参数检验
        ThrowUtils.throwIf(ObjectUtil.hasEmpty(userAccount,userPassword,checkPassword),new BusinessException(ErrorCode.PARAMS_ERROR));
        ThrowUtils.throwIf(userAccount.length() < 8,new BusinessException(ErrorCode.PARAMS_ERROR,"账号长度不小于8位"));
        ThrowUtils.throwIf(userPassword.length() < 8 || userPassword.length() > 16,new BusinessException(ErrorCode.PARAMS_ERROR,"密码长度为8~16位"));
        ThrowUtils.throwIf(!ObjectUtil.equal(checkPassword,userPassword),new BusinessException(ErrorCode.PARAMS_ERROR,"两次输入的密码不一样"));

        // 2.判断是否为已注册用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        ThrowUtils.throwIf(count(queryWrapper) > 0,new BusinessException(ErrorCode.PARAMS_ERROR,"账号重复"));

        // 3.密码加密
        String encryptPassword = getEncryptPassword(userPassword);

        // 4.插入数据库
        User user = User.builder().userAccount(userAccount)
                .userPassword(encryptPassword)
                .userRole(UserRoleEnum.USER.getValue())
                .userName("zcxt_" + UUID.randomUUID())
                .userAvatar("https://img2.baidu.com/it/u=3921464713,1750126262&fm=253&fmt=auto&app=138&f=PNG?w=500&h=500")
                .shareCode(RandomUtil.randomNumbers(10)).build();
        return save(user);
    }

    /**
     * 用户登录
     * 验证用户账号和密码，并返回用户信息的VO对象
     * @param userAccount 用户账号
     * @param userPassword 用户密码
     * @return UserVO 返回脱敏后的用户信息
     */
    @Override
    public UserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1.参数校验
        ThrowUtils.throwIf(ObjectUtil.hasEmpty(userAccount,userPassword),new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空"));
        ThrowUtils.throwIf(userAccount.length() < 8,new BusinessException(ErrorCode.PARAMS_ERROR,"账号长度不小于8位"));
        ThrowUtils.throwIf(userPassword.length() < 8 || userPassword.length() > 16,new BusinessException(ErrorCode.PARAMS_ERROR,"密码长度为8~16位"));

        // 2.密码加密
        String encryptPassword = getEncryptPassword(userPassword);

        // 3.查询用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount).eq("userPassword",encryptPassword);
        User user = getOne(queryWrapper);
        if(user == null){
            log.error("用户不存在或密码错误");
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户不存在或密码错误");
        }

        // 4.保存到session中
        HttpSession session = request.getSession();
        session.setAttribute("user_login_status",user);
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user,userVO);

        // 5.返回脱敏后的用户信息
        return userVO;
    }

    /**
     * 密码密文加密
     * @param userPassword 原密码
     * @return 密文密码
     */
    @Override
    public String getEncryptPassword(String userPassword){
        // 加盐，混淆密码
        return DigestUtils.md5DigestAsHex(("s_97&#!p" + userPassword).getBytes());
    }

    /**
     * 获取当前登录的用户信息
     * @param request HTTP请求对象，用于获取当前请求的上下文信息
     * @return 返回当前登录的User对象
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 1.查询用户
        HttpSession session = request.getSession();
        User currentUser = (User) session.getAttribute("user_login_status");

        // 2.判空
        ThrowUtils.throwIf(ObjectUtil.isEmpty(currentUser) || currentUser.getId() == null,new BusinessException(ErrorCode.NOT_LOGIN_ERROR));

        // 3.重新获取用户数据，确保数据一致性
        User user = query().eq("userAccount", currentUser.getUserAccount()).one();
        ThrowUtils.throwIf(ObjectUtil.isEmpty(user),new BusinessException(ErrorCode.NOT_LOGIN_ERROR));

        return user;
    }

    /**
     * 用户退出登录功能
     * @param request HTTP请求对象，用于访问会话属性
     * @return 总是返回true，表示退出登录操作执行了
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {

        // 1.先判断是否已登录
        Object userObj = request.getSession().getAttribute("user_login_status");
        if (userObj == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 2.移除登录状态
        request.getSession().removeAttribute("user_login_status");
        return true;
    }

    @Override
    public void userCreate(UserCreateDTO userCreateDTO) {
        // 1.参数校验
        ThrowUtils.throwIf(ObjectUtil.isEmpty(userCreateDTO),new BusinessException(ErrorCode.PARAMS_ERROR));
        ThrowUtils.throwIf(userCreateDTO.getUserAccount().length() < 8,new BusinessException(ErrorCode.PARAMS_ERROR,"账号长度不小于8位"));

        // 2.判断账号是否重复
        User user = query().eq("userAccount", userCreateDTO.getUserAccount()).one();
        ThrowUtils.throwIf(!ObjectUtil.isEmpty(user),new BusinessException(ErrorCode.PARAMS_ERROR,"账号重复"));

        // 3.插入记录
        user = BeanUtil.copyProperties(userCreateDTO,User.class);
        user.setUserPassword(getEncryptPassword("12345678"));
        user.setUserAvatar("https://img2.baidu.com/it/u=3921464713,1750126262&fm=253&fmt=auto&app=138&f=PNG?w=500&h=500");
        user.setUserName("zcxt_" + UUID.randomUUID());
        boolean isOk = save(user);
        ThrowUtils.throwIf(!isOk,new BusinessException(ErrorCode.OPERATION_ERROR,"插入失败"));
    }

    @Override
    public void userRemove(Long id) {
        // 1.参数校验
        ThrowUtils.throwIf((ObjectUtil.isEmpty(id) || id < 0),new BusinessException(ErrorCode.PARAMS_ERROR));
        // 2.判断删除用户是否存在
        User user = query().eq("id", id).one();
        ThrowUtils.throwIf(ObjectUtil.isEmpty(user),new BusinessException(ErrorCode.PARAMS_ERROR,"删除账号不存在"));
        boolean isOk = removeById(id);
        ThrowUtils.throwIf(!isOk,new BusinessException(ErrorCode.OPERATION_ERROR,"删除失败"));
    }

    @Override
    public void userUpdate(UserUpdateDTO userUpdateDTO) {
        // 1.参数校验
        ThrowUtils.throwIf(ObjectUtil.isEmpty(userUpdateDTO),new BusinessException(ErrorCode.PARAMS_ERROR));
        ThrowUtils.throwIf((ObjectUtil.hasEmpty(userUpdateDTO.getUserAccount(),userUpdateDTO.getId()) || userUpdateDTO.getUserAccount().length() < 8),new BusinessException(ErrorCode.PARAMS_ERROR));

        // 2.修改用户
        User user = BeanUtil.copyProperties(userUpdateDTO,User.class);
        boolean isOk = updateById(user);
        ThrowUtils.throwIf(!isOk,new BusinessException(ErrorCode.OPERATION_ERROR,"更新失败"));
    }

    @Override
    public User getUserById(Long id) {
        ThrowUtils.throwIf(id == null || id < 0,new BusinessException(ErrorCode.PARAMS_ERROR));
        User user = query().eq("id", id).one();
        ThrowUtils.throwIf(user == null,new BusinessException(ErrorCode.PARAMS_ERROR,"查询用户不存在"));
        return user;
    }

    @Override
    public IPage<UserVO> userPageQuery(UserQueryDTO userQueryDTO) {
        ThrowUtils.throwIf(userQueryDTO == null, ErrorCode.PARAMS_ERROR);
        // 设置分页查询条件
        Page<User> page = new Page<>(userQueryDTO.getCurrent(),userQueryDTO.getPageSize());
        // 设置其他查询条件
        QueryWrapper<User> queryWrapper = getQueryWrapper(userQueryDTO);
        // 调用mybatis-plus的mapper实现分页查询
        Page<User> userPage = userMapper.selectPage(page, queryWrapper);
        // 脱敏处理
        Page<UserVO> userVOPage = new Page<>(userQueryDTO.getCurrent(),userQueryDTO.getPageSize(),userPage.getTotal());
        List<UserVO> userVOList = getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return userVOPage;
    }

    private List<UserVO> getUserVOList(List<User> userList) {
        List<UserVO> userVOList = new ArrayList<>();
        for(User user : userList){
            UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
            userVOList.add(userVO);
        }
        return userVOList;
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryDTO userQueryDTO) {
        if (userQueryDTO == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryDTO.getId();
        String userAccount = userQueryDTO.getUserAccount();
        String userName = userQueryDTO.getUserName();
        String userRole = userQueryDTO.getUserRole();
        String sortField = userQueryDTO.getSortField();
        String sortOrder = userQueryDTO.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public Boolean isAdmin(User user) {
        if (user == null) {
            return false;
        }
        return user.getUserRole().equals(UserRoleEnum.ADMIN.getValue());
    }

}




