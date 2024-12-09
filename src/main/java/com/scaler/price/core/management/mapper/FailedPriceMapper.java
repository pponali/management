package com.scaler.price.core.management.mapper;

import com.scaler.price.core.management.domain.FailedPrice;
import com.scaler.price.core.management.dto.FailedPriceDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface FailedPriceMapper {
    FailedPriceMapper INSTANCE = Mappers.getMapper(FailedPriceMapper.class);

    FailedPriceDTO toDTO(FailedPrice failedPrice);
    FailedPrice toEntity(FailedPriceDTO failedPriceDTO);
}
