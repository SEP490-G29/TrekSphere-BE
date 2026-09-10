package com.sep.treksphere.matching.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupChecklistSummaryResponse {

    private int totalItems;
    private int completedItems;
    private int sharedItems;
    private int personalItems;
    private List<GroupChecklistItemResponse> items;
}
