package com.sep.treksphere.constant;

public class ValidationConstant {
    private ValidationConstant() {
        // Private constructor to prevent instantiation
    }

    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$";
    public static final String PASSWORD_MESSAGE = "Password must contain at least one letter, one number, and be at least 8 characters long";
}
