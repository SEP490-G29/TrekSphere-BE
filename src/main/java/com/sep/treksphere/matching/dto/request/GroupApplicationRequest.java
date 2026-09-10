package com.sep.treksphere.matching.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupApplicationRequest {

    @Size(max = 500, message = "Lời nhắn không được vượt quá 500 ký tự")
    private String message;
}
