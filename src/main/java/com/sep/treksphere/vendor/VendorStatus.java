package com.sep.treksphere.vendor;

public enum VendorStatus {
    PENDING, ACTIVE, SUSPENDED;

    public boolean canTransitionTo(VendorStatus target) {
        if (target == null) {
            return false;
        }
        return this == target
                || (this == PENDING && target == ACTIVE)
                || (this == ACTIVE && target == SUSPENDED)
                || (this == SUSPENDED && target == ACTIVE);
    }
}
