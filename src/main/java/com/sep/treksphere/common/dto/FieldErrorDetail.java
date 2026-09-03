package com.sep.treksphere.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class FieldErrorDetail {
    private String field;
    private String message;
}
