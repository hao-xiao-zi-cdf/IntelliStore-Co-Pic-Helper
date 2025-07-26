package com.hao_xiao_zi.intellistorecopichelper.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hao_xiao_zi.intellistorecopichelper.exception.BusinessException;
import com.hao_xiao_zi.intellistorecopichelper.exception.ErrorCode;
import com.hao_xiao_zi.intellistorecopichelper.exception.ThrowUtils;
import com.hao_xiao_zi.intellistorecopichelper.manager.auth.StpKit;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.user.UserCreateDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.user.UserEditDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.user.UserQueryDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.user.UserUpdateDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.User;
import com.hao_xiao_zi.intellistorecopichelper.model.enums.UserRoleEnum;
import com.hao_xiao_zi.intellistorecopichelper.model.vo.UserVO;
import com.hao_xiao_zi.intellistorecopichelper.service.UserService;
import com.hao_xiao_zi.intellistorecopichelper.mapper.UserMapper;
import com.hao_xiao_zi.intellistorecopichelper.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.hao_xiao_zi.intellistorecopichelper.constant.PasswordConstant.DEFAULT_PASSWORD;
import static com.hao_xiao_zi.intellistorecopichelper.constant.PasswordConstant.SALTED_STRING;
import static com.hao_xiao_zi.intellistorecopichelper.constant.SystemConstant.*;

/**
 * @author 34255
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-07-05 13:43:32
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    public UserMapper userMapper;

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
        ThrowUtils.throwIf(RegexUtils.isAccountInvalid(userAccount), new BusinessException(ErrorCode.PARAMS_ERROR, "输入的帐号不符合格式"));
        ThrowUtils.throwIf(RegexUtils.isPasswordInvalid(userPassword), new BusinessException(ErrorCode.PARAMS_ERROR, "输入的密码不符合格式"));
        ThrowUtils.throwIf(!ObjectUtil.equal(checkPassword, userPassword), new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致"));

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
                .userName(PROJECT_PREFIX + UUID.randomUUID())
                .userAvatar(DEFAULT_AVATAR)
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
        ThrowUtils.throwIf(RegexUtils.isAccountInvalid(userAccount), new BusinessException(ErrorCode.PARAMS_ERROR, "输入的账号不符合格式"));
        ThrowUtils.throwIf(RegexUtils.isPasswordInvalid(userPassword), new BusinessException(ErrorCode.PARAMS_ERROR, "输入的密码不符合格式"));

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

        // 4.保存到session
        HttpSession session = request.getSession();
        session.setAttribute(USER_LOGIN_STATUS, user);

        // 记录用户登录态到 Sa-token，便于空间鉴权时使用
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(USER_LOGIN_STATUS, user);

        // 5.返回脱敏后的用户信息
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user,userVO);
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
        return DigestUtils.md5DigestAsHex((SALTED_STRING + userPassword).getBytes());
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
        User currentUser = (User) session.getAttribute(USER_LOGIN_STATUS);

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
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATUS);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 2.移除登录状态
        request.getSession().removeAttribute(USER_LOGIN_STATUS);
        return true;
    }

    /**
     * 用户创建
     *
     * @param userCreateDTO 用户创建信息的数据传输对象
     *                      此方法负责处理用户创建逻辑，包括参数校验、检查账号是否重复以及插入用户数据
     */
    @Override
    public void userCreate(UserCreateDTO userCreateDTO) {
        // 1.参数校验
        ThrowUtils.throwIf(ObjectUtil.isEmpty(userCreateDTO),new BusinessException(ErrorCode.PARAMS_ERROR));
        ThrowUtils.throwIf(userCreateDTO.getUserAccount().length() < 8,new BusinessException(ErrorCode.PARAMS_ERROR,"账号长度不小于8位"));

        // 2.判断账号是否重复
        User user = query().eq("userAccount", userCreateDTO.getUserAccount()).one();
        ThrowUtils.throwIf(!ObjectUtil.isEmpty(user),new BusinessException(ErrorCode.PARAMS_ERROR,"账号重复"));

        // 3.插入记录
        user = BeanUtil.copyProperties(userCreateDTO, User.class);
        user.setUserPassword(getEncryptPassword(DEFAULT_PASSWORD));
        user.setUserAvatar(DEFAULT_AVATAR);
        user.setUserName(PROJECT_PREFIX + UUID.randomUUID());
        boolean isOk = save(user);
        ThrowUtils.throwIf(!isOk, new BusinessException(ErrorCode.OPERATION_ERROR, "插入失败"));
    }

    /**
     * 根据id删除用户
     *
     * @param id 用户id
     */
    @Override
    public void userRemove(Long id) {
        // 1.参数校验
        ThrowUtils.throwIf((ObjectUtil.isEmpty(id) || id < 0),new BusinessException(ErrorCode.PARAMS_ERROR));
        // 2.判断删除用户是否存在
        User user = query().eq("id", id).one();
        ThrowUtils.throwIf(ObjectUtil.isEmpty(user),new BusinessException(ErrorCode.PARAMS_ERROR,"删除账号不存在"));
        boolean isOk = removeById(id);
        ThrowUtils.throwIf(!isOk, new BusinessException(ErrorCode.OPERATION_ERROR, "删除失败"));
    }

    /**
     * 更新用户信息
     *
     * @param userUpdateDTO 用户更新数据传输对象，包含需要更新的用户信息
     */
    @Override
    public void userUpdate(UserUpdateDTO userUpdateDTO) {
        // 1.参数校验
        ThrowUtils.throwIf(ObjectUtil.isEmpty(userUpdateDTO),new BusinessException(ErrorCode.PARAMS_ERROR));
        ThrowUtils.throwIf((ObjectUtil.hasEmpty(userUpdateDTO.getUserAccount(), userUpdateDTO.getId()) || RegexUtils.isAccountInvalid(userUpdateDTO.getUserAccount())), new BusinessException(ErrorCode.PARAMS_ERROR));

        // 2.设置条件构造器
        UpdateWrapper<User> wrapper = getUseUpdateWrapper(userUpdateDTO);

        // 更新
        boolean isOk = update(wrapper);
        ThrowUtils.throwIf(!isOk, new BusinessException(ErrorCode.OPERATION_ERROR, "更新失败"));
    }

    /**
     * 编辑用户信息的方法
     *
     * @param userEditDTO 包含用户编辑信息的数据传输对象
     * @param request     用于获取登录用户信息，判断是否为本人操作
     */
    @Override
    public void userEdit(UserEditDTO userEditDTO, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(ObjectUtil.isEmpty(userEditDTO), new BusinessException(ErrorCode.PARAMS_ERROR));
        String id = userEditDTO.getId();
        String account = userEditDTO.getUserAccount();
        String password = userEditDTO.getUserPassword();
        String phone = userEditDTO.getPhone();
        String email = userEditDTO.getEmail();
        String profile = userEditDTO.getUserProfile();

        ThrowUtils.throwIf(account != null && RegexUtils.isAccountInvalid(account), new BusinessException(ErrorCode.PARAMS_ERROR, "输入的账号不符合格式"));
        ThrowUtils.throwIf(password != null && RegexUtils.isPasswordInvalid(password), new BusinessException(ErrorCode.PARAMS_ERROR, "输入的密码不符合格式"));
        ThrowUtils.throwIf(phone != null && RegexUtils.isPhoneInvalid(phone), new BusinessException(ErrorCode.PARAMS_ERROR, "输入的手机号不符合格式"));
        ThrowUtils.throwIf(email != null && RegexUtils.isEmailInvalid(email), new BusinessException(ErrorCode.PARAMS_ERROR, "输入的邮箱不符合格式"));

        // 判断是否为本人操作
        ThrowUtils.throwIf(!userEditDTO.getId().equals(getLoginUser(request).getId()), new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限，非本人操作"));

        // 设置条件构造器
        UserUpdateDTO userUpdateDTO = BeanUtil.copyProperties(userEditDTO, UserUpdateDTO.class);
        UpdateWrapper<User> wrapper = getUseUpdateWrapper(userUpdateDTO);
        wrapper.set(StrUtil.isNotBlank(password), "userPassword", password);
        wrapper.set(StrUtil.isNotBlank(phone), "phone", phone);
        wrapper.set(StrUtil.isNotBlank(email), "email", email);
        wrapper.set(StrUtil.isNotBlank(profile), "userProfile", profile);
        wrapper.set("editTime", Timestamp.valueOf(LocalDateTime.now()));

        // 编辑
        boolean isOk = update(wrapper);
        ThrowUtils.throwIf(!isOk,new BusinessException(ErrorCode.OPERATION_ERROR,"编辑信息失败"));
    }

    /**
     * 根据用户更新DTO获取用户更新条件包装器
     * 该方法用于构建一个UpdateWrapper，用于后续的用户信息更新操作
     * 它将根据用户更新DTO中的信息，设置更新条件和更新内容
     *
     * @param userUpdateDTO 用户更新DTO，包含需要更新的用户信息
     * @return UpdateWrapper<User> 用户更新条件包装器，用于执行更新操作
     */
    @Override
    public UpdateWrapper<User> getUseUpdateWrapper(UserUpdateDTO userUpdateDTO) {
        UpdateWrapper<User> wrapper = new UpdateWrapper<>();

        Long id = userUpdateDTO.getId();
        String userAccount = userUpdateDTO.getUserAccount();
        String userName = userUpdateDTO.getUserName();
        String userAvatar = userUpdateDTO.getUserAvatar();
        String userRole = userUpdateDTO.getUserRole();

        wrapper.set(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        wrapper.set(StrUtil.isNotBlank(userName), "userName", userName);
        wrapper.set(StrUtil.isNotBlank(userAvatar), "userAvatar", userAvatar);
        wrapper.set(StrUtil.isNotBlank(userRole), "userRole", userRole);
        wrapper.eq("id", id);
        return wrapper;
    }

    /**
     * 根据用户id查询用户
     * @param id 用户id
     * @return 用户
     */
    @Override
    public User getUserById(Long id) {
        ThrowUtils.throwIf(id == null || id < 0,new BusinessException(ErrorCode.PARAMS_ERROR));
        User user = query().eq("id", id).one();
        ThrowUtils.throwIf(user == null,new BusinessException(ErrorCode.PARAMS_ERROR,"查询用户不存在"));
        return user;
    }

    /**
     * 获取脱敏后的用户信息列表
     *
     * @param userQueryDTO 用户查询条件封装对象，包含分页信息和查询过滤条件
     * @return 返回分页查询结果，包含脱敏后的用户信息的列表和总记录数
     */
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

    /**
     * 将用户对象列表转换为用户视图对象(脱敏)列表
     *
     * @param userList 用户对象列表，不能为空
     * @return 用户视图对象列表(脱敏)
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        List<UserVO> userVOList = new ArrayList<>();
        for(User user : userList){
            UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
            userVOList.add(userVO);
        }
        return userVOList;
    }

    /**
     * 获取用户查询条件构造器
     * 该方法用于构建用户表的查询条件，根据传入的用户查询DTO中的不同字段进行条件组装
     *
     * @param userQueryDTO 用户查询数据传输对象，包含查询所需的各个字段
     * @return 返回一个QueryWrapper对象，用于执行后续的数据库查询操作
     */
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

    /**
     * 判断当前用户是否为管理员
     * @param user 当前用户
     * @return 是否
     */
    @Override
    public Boolean isAdmin(User user) {
        if (user == null) {
            return false;
        }
        return user.getUserRole().equals(UserRoleEnum.ADMIN.getValue());
    }

}




