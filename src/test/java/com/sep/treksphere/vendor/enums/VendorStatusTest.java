package com.sep.treksphere.vendor;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VendorStatusTest {

    @ParameterizedTest
    @CsvSource({
            "PENDING,PENDING,true",
            "PENDING,ACTIVE,true",
            "PENDING,SUSPENDED,false",
            "ACTIVE,PENDING,false",
            "ACTIVE,ACTIVE,true",
            "ACTIVE,SUSPENDED,true",
            "SUSPENDED,PENDING,false",
            "SUSPENDED,ACTIVE,true",
            "SUSPENDED,SUSPENDED,true"
    })
    void enforcesStatusTransitionMatrix(
            VendorStatus current, VendorStatus target, boolean expected) {
        assertEquals(expected, current.canTransitionTo(target));
    }
}
