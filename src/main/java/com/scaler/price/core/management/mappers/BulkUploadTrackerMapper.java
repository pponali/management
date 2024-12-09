package com.scaler.price.core.management.mappers;

import com.scaler.price.core.management.domain.BulkUploadTracker;
import com.scaler.price.core.management.dto.BulkPriceUploadDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;


@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BulkUploadTrackerMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    BulkUploadTracker toEntity(BulkPriceUploadDTO dto);

    BulkPriceUploadDTO toDTO(BulkUploadTracker entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    BulkUploadTracker updateEntity(@MappingTarget BulkUploadTracker existingBulkUploadTracker, BulkPriceUploadDTO dto);
}
