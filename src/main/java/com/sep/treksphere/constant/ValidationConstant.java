package com.sep.treksphere.constant;

public class ValidationConstant {
    private ValidationConstant() {
    }

    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$";
    public static final String PASSWORD_MESSAGE = "Password must contain at least one letter, one number, and be at least 8 characters long";
    public static final String PHONE_REGEX = "^$|^(0|\\+84)(\\s|\\.)?((3[2-9])|(5[689])|(7[06-9])|(8[1-689])|(9[0-46-9]))(\\d)(\\s|\\.)?(\\d{3})(\\s|\\.)?(\\d{3})$";
}
