package com.sep.treksphere.utils;

import com.sep.treksphere.constant.ValidationConstant;
import com.sep.treksphere.exception.AppException;
import com.sep.treksphere.exception.ErrorCode;

import java.math.BigDecimal;

public class GeoUtils {


    public static double calculateDistanceInMeters(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            throw new AppException(ErrorCode.GPS_COORDINATES_REQUIRED);
        }

        double lat1Val = lat1.doubleValue();
        double lon1Val = lon1.doubleValue();
        double lat2Val = lat2.doubleValue();
        double lon2Val = lon2.doubleValue();

        double lat1Rad = Math.toRadians(lat1Val);
        double lon1Rad = Math.toRadians(lon1Val);
        double lat2Rad = Math.toRadians(lat2Val);
        double lon2Rad = Math.toRadians(lon2Val);

        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return ValidationConstant.EARTH_RADIUS_METERS * c;
    }


    public static boolean isWithinAllowedRadius(BigDecimal actualLat, BigDecimal actualLon, BigDecimal configLat, BigDecimal configLon) {
        double distance = calculateDistanceInMeters(actualLat, actualLon, configLat, configLon);
        return distance <= ValidationConstant.ALLOWED_CHECKIN_RADIUS_METERS;
    }
}
