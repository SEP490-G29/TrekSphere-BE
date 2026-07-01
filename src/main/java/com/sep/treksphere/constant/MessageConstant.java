package com.sep.treksphere.constant;

public class MessageConstant {
    private MessageConstant() {
        // Private constructor to prevent instantiation
    }

    // Auth Messages
    public static final String USER_NOT_FOUND = "User not found";
    public static final String USER_NOT_LOGGED_IN = "User must be logged in";
    public static final String CURRENT_PASSWORD_INCORRECT = "Current password is incorrect";
    public static final String NEW_PASSWORD_SAME_AS_OLD = "New password cannot be the same as the current password";
    public static final String PASSWORD_CHANGED_SUCCESSFULLY = "Password has been changed successfully.";
    
    // Validation Messages
    public static final String CURRENT_PASSWORD_REQUIRED = "Current password is required";
    public static final String NEW_PASSWORD_REQUIRED = "New password is required";
    public static final String PASSWORD_MIN_LENGTH = "Password must be at least 8 characters long";
}
