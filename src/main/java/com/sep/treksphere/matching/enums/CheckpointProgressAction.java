package com.sep.treksphere.matching.enums;

/** Hành động leader có thể chủ động chọn khi cập nhật tiến độ checkpoint (không gồm PENDING — đó là trạng thái mặc định/gỡ về). */
public enum CheckpointProgressAction {
    CHECKED_IN, SKIPPED
}
