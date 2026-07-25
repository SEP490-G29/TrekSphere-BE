package com.sep.treksphere.constant;

public class ValidationConstant {
    private ValidationConstant() {
    }

    public static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_\\-+=\\[\\]{}|\\\\:;\"'<>,.?/~`]).{8,}$";
    public static final String PHONE_REGEX = "^$|^(0|\\+84)(\\s|\\.)?((3[2-9])|(5[689])|(7[06-9])|(8[1-689])|(9[0-46-9]))(\\d)(\\s|\\.)?(\\d{3})(\\s|\\.)?(\\d{3})$";
    public static final String TAX_CODE_REGEX = "^[0-9]{10,13}$";
    public static final String VENDOR_PHONE_REGEX = "^(1800|1900|0|\\+84)[0-9]{4,11}$";

    public static final String MIN_LATITUDE = "-90.0";
    public static final String MAX_LATITUDE = "90.0";
    public static final String MIN_LONGITUDE = "-180.0";
    public static final String MAX_LONGITUDE = "180.0";

    public static final double ALLOWED_CHECKIN_RADIUS_METERS = 200.0;
    public static final double EARTH_RADIUS_METERS = 6371000.0;
}
