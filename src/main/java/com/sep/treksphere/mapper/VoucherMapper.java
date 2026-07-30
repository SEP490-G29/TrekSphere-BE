package com.sep.treksphere.mapper;

import com.sep.treksphere.dto.response.VoucherResponse;
import com.sep.treksphere.entity.Voucher;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import com.sep.treksphere.dto.request.CreateVoucherRequest;

import org.mapstruct.BeanMapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import com.sep.treksphere.dto.request.UpdateVoucherRequest;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VoucherMapper {
    VoucherResponse toVoucherResponse(Voucher voucher);
    Voucher toVoucher(CreateVoucherRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateVoucherFromRequest(UpdateVoucherRequest request, @MappingTarget Voucher voucher);
}
