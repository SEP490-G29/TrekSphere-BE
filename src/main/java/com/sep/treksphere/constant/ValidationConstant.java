package com.sep.treksphere.constant;

public class ValidationConstant {
    private ValidationConstant() {
    }

    public static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_\\-+=\\[\\]{}|\\\\:;\"'<>,.?/~`]).{8,}$";
    public static final String PASSWORD_MESSAGE = "Mật khẩu phải chứa ít nhất một chữ cái, một số, một chữ số và có độ dài tối thiểu 8 ký tự.";
    public static final String PHONE_REGEX = "^$|^(0|\\+84)(\\s|\\.)?((3[2-9])|(5[689])|(7[06-9])|(8[1-689])|(9[0-46-9]))(\\d)(\\s|\\.)?(\\d{3})(\\s|\\.)?(\\d{3})$";
}
