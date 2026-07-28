package com.sep.treksphere.mapper;

import com.sep.treksphere.dto.response.VoucherResponse;
import com.sep.treksphere.entity.Voucher;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VoucherMapper {
    VoucherResponse toVoucherResponse(Voucher voucher);
}
