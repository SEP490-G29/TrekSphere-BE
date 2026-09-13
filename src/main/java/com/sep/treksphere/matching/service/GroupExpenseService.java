package com.sep.treksphere.matching.service;

import com.sep.treksphere.common.dto.PaginationResponse;
import com.sep.treksphere.matching.dto.request.GroupExpenseCreateRequest;
import com.sep.treksphere.matching.dto.request.GroupExpenseFilterRequest;
import com.sep.treksphere.matching.dto.request.GroupExpenseUpdateRequest;
import com.sep.treksphere.matching.dto.response.GroupExpenseResponse;
import com.sep.treksphere.matching.dto.response.GroupExpenseSummaryResponse;

import java.util.UUID;

public interface GroupExpenseService {

    GroupExpenseResponse createExpense(UUID groupId, GroupExpenseCreateRequest request, UUID currentUserId);

    GroupExpenseResponse updateExpense(UUID groupId, UUID expenseId, GroupExpenseUpdateRequest request, UUID currentUserId);

    void voidExpense(UUID groupId, UUID expenseId, UUID currentUserId);

    GroupExpenseResponse getExpenseDetail(UUID groupId, UUID expenseId, UUID currentUserId);

    PaginationResponse<GroupExpenseResponse> getGroupExpenses(UUID groupId, GroupExpenseFilterRequest filter, UUID currentUserId);

    GroupExpenseSummaryResponse getExpenseSummary(UUID groupId, UUID currentUserId);
}
