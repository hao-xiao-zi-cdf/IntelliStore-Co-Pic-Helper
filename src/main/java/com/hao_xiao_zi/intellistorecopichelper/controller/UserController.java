package com.hao_xiao_zi.intellistorecopichelper.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hao_xiao_zi.intellistorecopichelper.common.BaseResponse;
import com.hao_xiao_zi.intellistorecopichelper.common.PageResult;
import com.hao_xiao_zi.intellistorecopichelper.common.ResultUtils;
import com.hao_xiao_zi.intellistorecopichelper.exception.BusinessException;
import com.hao_xiao_zi.intellistorecopichelper.exception.ErrorCode;
import com.hao_xiao_zi.intellistorecopichelper.exception.ThrowUtils;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.user.UserLoginDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.user.UserQueryDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.user.UserRegisterDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.user.UserUpdateDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.User;
import com.hao_xiao_zi.intellistorecopichelper.model.vo.UserVO;
import com.hao_xiao_zi.intellistorecopichelper.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-05
 * Time: 10:06
 */
@Slf4j
@RestController
@RequestMapping("/user")
@Api(tags = "用户模块相关接口")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     *
     * @param userRegisterDTO 用户注册信息的数据传输对象
     * @return 响应结果
     */
    @PostMapping("/register")
    @ApiOperation("用户注册")
    public BaseResponse<String> userRegister(@RequestBody UserRegisterDTO userRegisterDTO) {
        if (ObjectUtil.isEmpty(userRegisterDTO)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        String userAccount = userRegisterDTO.getUserAccount();
        String userPassword = userRegisterDTO.getUserPassword();
        String checkPassword = userRegisterDTO.getCheckPassword();
        Boolean isOk = userService.userRegister(userAccount, userPassword, checkPassword);
        ThrowUtils.throwIf(!isOk,new BusinessException(ErrorCode.SYSTEM_ERROR,"系统内部异常"));
        return ResultUtils.success("success");
    }

    /**
     * 用户登录接口
     *
     * @param userLoginDTO 登录请求数据传输对象，包含用户账号和密码
     * @param request      HTTP请求对象，用于获取请求信息
     * @return BaseResponse<UserVO> 返回封装的响应结果，包含用户视图对象
     */
    @PostMapping("/login")
    @ApiOperation("用户登录")
    public BaseResponse<UserVO> userLogin(@RequestBody UserLoginDTO userLoginDTO, HttpServletRequest request){
        ThrowUtils.throwIf(ObjectUtil.isEmpty(userLoginDTO),new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空"));
        String userAccount = userLoginDTO.getUserAccount();
        String userPassword = userLoginDTO.getUserPassword();
        UserVO userVO = userService.userLogin(userAccount, userPassword,request);
        return ResultUtils.success(userVO);
    }

    /**
     * 获取当前登录用户信息的接口
     *
     * @param request 用于获取请求信息的HttpServletRequest对象
     * @return 返回一个包含用户信息的BaseResponse对象，其中UserVO是用户信息的视图对象
     */
    @GetMapping("/login/get")
    @ApiOperation("获取登录用户信息")
    public BaseResponse<UserVO> getLoginUser(HttpServletRequest request){
        User user = userService.getLoginUser(request);
        return ResultUtils.success(BeanUtil.copyProperties(user,UserVO.class));
    }

    /**
     * 用户注销接口
     *
     * @param request HTTP请求对象，用于获取当前用户的会话信息
     * @return BaseResponse<Boolean> 返回封装的响应结果，表示用户注销是否成功
     */
    @PostMapping("/logout")
    @ApiOperation("用户注销")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    @GetMapping("/{id}")
    @ApiOperation("查询用户信息（管理员）")
    public BaseResponse<User> getUser(@PathVariable Long id){
        User user = userService.getUserById(id);
        user.setUserPassword(null);
        return ResultUtils.success(user);
    }

    @GetMapping("/vo/{id}")
    @ApiOperation("查询用户信息（用户）")
    public BaseResponse<UserVO> getUserVO(@PathVariable Long id){
        User user = userService.getUserById(id);
        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
        return ResultUtils.success(userVO);
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除用户")
    public BaseResponse<Boolean> deleteUser(@PathVariable Long id){
        userService.userRemove(id);
        return ResultUtils.success(true);
    }

    @PutMapping
    @ApiOperation("更新用户")
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateDTO userUpdateDTO){
        userService.userUpdate(userUpdateDTO);
        return ResultUtils.success(true);
    }

    /**
     * 查询用户列表接口
     * 该接口用于获取符合特定条件的用户列表，通过POST请求接收用户查询参数
     *
     * @param userQueryDTO 用户查询参数对象，包含查询用户所需的条件和分页信息
     * @return 返回包含用户列表和分页信息的响应对象
    */
    @PostMapping("/list")
    @ApiOperation("查询用户列表")
    public BaseResponse<PageResult> getUserList(@RequestBody UserQueryDTO userQueryDTO){
        // 调用用户服务的分页查询方法，获取用户列表和总记录数
        Page<UserVO> userVoPage = (Page<UserVO>)userService.userPageQuery(userQueryDTO);
        // 使用ResultUtils工具类构建成功响应，包含用户列表和分页信息
        return ResultUtils.success(new PageResult(userVoPage.getTotal(),userVoPage.getRecords()));
    }
}
