package com.hao_xiao_zi.intellistorecopichelper.utils;

import cn.hutool.core.util.StrUtil;

/**
 * @author 34255
 */
public class RegexUtils {

    /**
     * 是否是无效账号
     *
     * @param account 要校验的账号
     * @return true:不符合，false：符合
     */
    public static boolean isAccountInvalid(String account) {
        return mismatch(account, RegexPatterns.ACCOUNT_REGEX);
    }

    /**
     * 是否是无效密码格式
     *
     * @param password 要校验的密码
     * @return true:不符合，false：符合
     */
    public static boolean isPasswordInvalid(String password) {
        return mismatch(password, RegexPatterns.PASSWORD_REGEX);
    }

    /**
     * 是否是无效手机格式
     *
     * @param phone 要校验的手机号
     * @return true:不符合，false：符合
     */
    public static boolean isPhoneInvalid(String phone) {
        return mismatch(phone, RegexPatterns.PHONE_REGEX);
    }

    /**
     * 是否是无效邮箱格式
     *
     * @param email 要校验的邮箱
     * @return true:不符合，false：符合
     */
    public static boolean isEmailInvalid(String email) {
        return mismatch(email, RegexPatterns.EMAIL_REGEX);
    }

    /**
     * 检查字符串是否与给定的正则表达式不匹配
     *
     * @param str   待检查的字符串
     * @param regex 正则表达式
     * @return 如果字符串与正则表达式匹配，则返回true；否则返回false
     */
    private static boolean mismatch(String str, String regex) {
        if (StrUtil.isBlank(str)) {
            return false;
        }
        return str.matches(regex);
    }
}
