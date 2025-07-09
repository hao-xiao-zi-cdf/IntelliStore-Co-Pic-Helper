package com.hao_xiao_zi.intellistorecopichelper.utils;

/**
 * @author 34255
 */
public abstract class RegexPatterns {

    /**
     * 账号：6~12位，只能包含数字、字母（大小写均可）、下划线
     */
    public static final String ACCOUNT_REGEX = "^[a-zA-Z0-9_]{6,12}$";

    /**
     * 密码：8~16位，必须包含数字、小写字母、大写字母、特殊字符
     */
    public static final String PASSWORD_REGEX = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,16}";

    /**
     * 手机号：手机号码（中国大陆）：11位，以13、14、15、17、18、19开头
     */
    public static final String PHONE_REGEX = "^1[3-9]\\d{9}$\n";

    /**
     * 邮箱：支持基础邮箱格式，不支持 + 或多级子域名
     */
    public static final String EMAIL_REGEX = "^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$";
}
