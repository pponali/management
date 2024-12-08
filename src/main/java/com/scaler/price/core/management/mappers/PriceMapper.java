package com.scaler.price.core.management.mappers;

import com.scaler.price.core.management.domain.Price;
import com.scaler.price.core.management.dto.PriceDTO;
import com.scaler.price.rule.service.SellerService;
import com.scaler.price.rule.service.SiteService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PriceMapper {

    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "isSellerActive", expression = "java(sellerService.isSellerActive(dto.getSellerId()))")
    @Mapping(target = "isSiteActive", expression = "java(siteService.isSiteActive(dto.getSiteId()))")
    Price toEntity(PriceDTO dto, SellerService sellerService, SiteService siteService);

    PriceDTO toDTO(Price entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Price updateEntity(@MappingTarget Price existingPrice, PriceDTO dto);   
}
