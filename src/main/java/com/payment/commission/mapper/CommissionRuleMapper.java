package com.payment.commission.mapper;

import com.payment.commission.domain.entity.CommissionRule;
import com.payment.common.dto.commission.response.CommissionRuleResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper for CommissionRule entity to DTO conversions
 */
@Mapper(componentModel = "spring")
public interface CommissionRuleMapper {

    /**
     * Convert CommissionRule entity to CommissionRuleResponse DTO
     */
    CommissionRuleResponse toResponse(CommissionRule rule);
}
